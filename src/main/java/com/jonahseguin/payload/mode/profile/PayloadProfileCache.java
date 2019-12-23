/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.jonahseguin.lang.LangDefinitions;
import com.jonahseguin.lang.LangModule;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.base.sync.SyncService;
import com.jonahseguin.payload.base.type.PayloadInstantiator;
import com.jonahseguin.payload.base.uuid.UUIDService;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import com.jonahseguin.payload.mode.profile.store.ProfileStoreLocal;
import com.jonahseguin.payload.mode.profile.store.ProfileStoreMongo;
import lombok.Getter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@Singleton
public class PayloadProfileCache<X extends PayloadProfile> extends PayloadCache<UUID, X, NetworkProfile> implements LangModule, ProfileCache<X> {

    private final ProfileCacheSettings settings = new ProfileCacheSettings();
    private final ConcurrentMap<UUID, PayloadProfileController<X>> controllers = new ConcurrentHashMap<>();
    private final ProfileStoreLocal<X> localStore = new ProfileStoreLocal<>(this);
    private final ProfileStoreMongo<X> mongoStore = new ProfileStoreMongo<>(this);
    @Inject private UUIDService uuidService;

    public PayloadProfileCache(Injector injector, PayloadInstantiator<UUID, X> instantiator, String name, Class<X> payload) {
        super(injector, instantiator, name, UUID.class, payload, NetworkProfile.class);
        this.setupModule();
        lang.register(this);
    }

    @Override
    protected void setupModule() {
        super.injectMe();
        super.setupModule();
        injector.injectMembers(this);
    }

    @Override
    public void define(LangDefinitions l) {
        l.define("deny-join-database", "&cThe database is currently offline.  We are working on resolving this issue as soon as possible, please try again soon.");
        l.define("no-profile", "&cYour profile is not loaded.  Please wait as we will continue to attempt to load it.");
        l.define("shutdown", "&cThe server has shutdown.");
    }

    @Override
    public String langModule() {
        return "profile-cache";
    }

    @Override
    protected boolean initialize() {
        boolean success = true;
        if (!localStore.start()) {
            success = false;
            errorService.capture("Failed to start Local store for cache " + name);
        }
        if (!mongoStore.start()) {
            success = false;
            errorService.capture("Failed to start MongoDB store for cache " + name);
        }
        if (mode.equals(PayloadMode.NETWORK_NODE)) {
            handshakeService.subscribe(new ProfileHandshake(injector, this));
        }
        database.getMorphia().map(NetworkProfile.class);
        return success;
    }

    @Override
    protected boolean terminate() {
        boolean success = true;
        AtomicInteger failedSaves = new AtomicInteger(0);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            getFromCache(player).ifPresent(payload -> {
                if (!save(payload)) {
                    failedSaves.getAndIncrement();
                }
            });
        }
        if (failedSaves.get() > 0) {
            errorService.capture(failedSaves + " objects failed to save during shutdown");
        }
        controllers.clear();
        if (!localStore.shutdown()) {
            success = false;
        }
        if (!mongoStore.shutdown()) {
            success = false;
        }


        return success;
    }

    @Override
    public Optional<NetworkProfile> getNetworked(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return networkService.get(key);
    }

    @Override
    public Optional<NetworkProfile> getNetworked(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        return networkService.get(payload);
    }

    @Override
    public Future<Optional<X>> getAsync(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return runAsync(() -> get(key));
    }

    @Override
    public Future<Optional<X>> getAsync(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        return runAsync(() -> get(username));
    }

    @Override
    public Future<Optional<X>> getAsync(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return runAsync(() -> get(player));
    }

    @Override
    public void uncache(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        getLocalStore().remove(key);
    }

    @Override
    public void uncache(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        getLocalStore().remove(payload);
    }

    @Override
    public void delete(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        getLocalStore().remove(payload);
        getDatabaseStore().remove(payload);
    }

    @Nonnull
    @Override
    public Collection<X> getCached() {
        return localStore.getAll();
    }

    @Nonnull
    @Override
    public SyncService<UUID, X, NetworkProfile> getSyncService() {
        return sync;
    }

    @Nonnull
    @Override
    public PayloadStore<UUID, X> getDatabaseStore() {
        return mongoStore;
    }

    @Override
    public Optional<X> get(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        Preconditions.checkState(username.length() > 1, "Username length must be > 1");
        Player player = plugin.getServer().getPlayerExact(username);
        if (player != null && player.isOnline()) {
            return get(player);
        }
        UUID uuid = uuidService.get(username).orElse(null);
        if (uuid != null) {
            if (isCached(uuid)) {
                return getFromCache(uuid);
            }
        }
        return mongoStore.getByUsername(username);
    }

    @Override
    public boolean isCached(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        Player player = plugin.getServer().getPlayerExact(username);
        if (player != null && player.isOnline()) {
            return isCached(player);
        }
        return uuidService.get(username).filter(this::isCached).isPresent();
    }

    @Override
    public void cache(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        Optional<X> o = getLocalStore().get(payload.getUniqueId());
        if (o.isPresent()) {
            updatePayloadFromNewer(o.get(), payload);
        } else {
            getLocalStore().save(payload);
        }
    }

    @Override
    public Optional<X> get(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return get(player.getUniqueId());
    }

    @Override
    public boolean isCached(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return isCached(player.getUniqueId());
    }

    @Override
    public Optional<X> get(@Nonnull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);
        PayloadProfileController<X> controller = this.controller(uniqueId);
        controller.setLogin(false);
        return controller.cache();
    }

    @Nonnull
    @Override
    public Set<X> getOnline() {
        return this.localStore.getLocalCache().values().stream()
                .filter(PayloadProfile::isOnline)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isCached(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return this.localStore.has(key);
    }

    @Override
    public Optional<X> getFromCache(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return this.localStore.get(key);
    }

    @Override
    public Optional<X> getFromDatabase(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return mongoStore.get(key);
    }

    @Override
    public Optional<X> getFromCache(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        UUID uuid = null;
        Player player = plugin.getServer().getPlayerExact(username);
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            Optional<UUID> o = uuidService.get(username);
            if (o.isPresent()) {
                uuid = o.get();
            }
        }
        if (uuid != null) {
            return getFromCache(uuid);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<X> getFromCache(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return getFromCache(player.getUniqueId());
    }

    @Override
    public Optional<X> getFromDatabase(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        return mongoStore.getByUsername(username);
    }

    @Override
    public Optional<X> getFromDatabase(@Nonnull Player player) {
        Preconditions.checkNotNull(player);
        return mongoStore.get(player.getUniqueId());
    }

    @Override
    public void delete(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        localStore.remove(key);
        mongoStore.remove(key);
    }

    @Override
    public void cacheAll() {
        this.getAll().forEach(this::cache);
    }

    @Override
    public UUID keyFromString(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        return UUID.fromString(key);
    }

    @Override
    public String keyToString(@Nonnull UUID key) {
        return key.toString();
    }

    @Nonnull
    public Set<X> getAll() {
        final Set<X> all = this.localStore.getAll().stream().filter(PayloadProfile::isOnline).collect(Collectors.toSet());
        all.addAll(this.mongoStore.getAll().stream().filter(x -> all.stream().noneMatch(x2 -> x.getUniqueId().equals(x2.getUniqueId()))).collect(Collectors.toSet()));
        return all;
    }

    @Nonnull
    @Override
    public PayloadProfileController<X> controller(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return controllers.computeIfAbsent(key, s -> new PayloadProfileController<>(this, s));
    }

    @Override
    public Future<Boolean> saveAsync(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        this.cache(payload);
        return this.runAsync(() -> this.save(payload));
    }

    @Override
    public boolean save(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        Optional<NetworkProfile> onp = networkService.get(payload);
        if (onp.isPresent()) {
            NetworkProfile np = onp.get();
            if (this.saveNoSync(payload)) {
                np.markSaved();
                if (networkService.save(np)) {
                    if (settings.isEnableSync()) {
                        sync.update(payload.getIdentifier());
                    }
                    return true;
                } else {
                    errorService.capture("Failed to save profile " + payload.getName() + ": Couldn't save network profile (but saved normal profile)");
                    return false;
                }
            } else {
                errorService.capture("Failed to save profile " + payload.getName() + ": Failed to save to database (via saveNoSync())");
                return false;
            }
        } else {
            errorService.capture("Failed to save profile " + payload.getName() + ": Network Profile doesn't exist");
            return false;
        }
    }

    @Override
    public boolean saveNoSync(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        cache(payload);
        if (mongoStore.save(payload)) {
            payload.setSaveFailed(false);
            payload.setLastSaveTimestamp(System.currentTimeMillis());
            payload.interact();
            return true;
        } else {
            payload.setSaveFailed(true);
            return false;
        }
    }

    @Override
    public int saveAll() {
        int failures = 0;
        for (Player p : this.getPlugin().getServer().getOnlinePlayers()) {
            Optional<X> o = this.get(p);
            if (o.isPresent()) {
                X payload = o.get();
                payload.interact();
                if (!this.save(payload)) {
                    failures++;
                }
            } else {
                failures++;
            }
        }
        return failures;
    }

    public PayloadProfileController<X> getController(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        return this.controllers.get(uuid);
    }

    public void removeController(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        this.controllers.remove(uuid);
    }

    @Override
    public boolean requireRedis() {
        return true;
    }

    @Override
    public boolean requireMongoDb() {
        return true;
    }

    @Nonnull
    @Override
    public ProfileCacheSettings getSettings() {
        return this.settings;
    }

    @Override
    public void updatePayloadID() {
        for (X x : this.getCached()) {
            x.setPayloadId(api.getPayloadID());
            getNetworked(x).ifPresent(np -> {
                if (np.isOnlineThisServer()) {
                    np.setLastSeenServer(serverService.getThisServer().getName());
                    getNetworkService().save(np);
                }
            });
        }
        this.pool.submit(this::saveAll);
    }

    @Override
    public NetworkProfile createNetworked() {
        return injector.getInstance(NetworkProfile.class);
    }
}
