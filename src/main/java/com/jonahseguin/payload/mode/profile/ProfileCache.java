/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.failsafe.FailedPayload;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.base.sync.SyncMode;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.mode.profile.handshake.HandshakeEvent;
import com.jonahseguin.payload.mode.profile.handshake.HandshakeListener;
import com.jonahseguin.payload.mode.profile.handshake.HandshakeManager;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerLocal;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerMongo;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerRedis;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Getter
public class ProfileCache<X extends PayloadProfile> extends PayloadCache<UUID, X, ProfileData> {

    private final ProfileCacheSettings settings = new ProfileCacheSettings();
    private final ConcurrentMap<UUID, PayloadProfileController<X>> controllers = new ConcurrentHashMap<>();
    private final HandshakeManager<X> handshakeManager = new HandshakeManager<>(this);
    private final HandshakeListener<X> handshakeListener = new HandshakeListener<>(this);
    private final ProfileLayerLocal<X> localLayer = new ProfileLayerLocal<>(this);
    private final ProfileLayerRedis<X> redisLayer = new ProfileLayerRedis<>(this);
    private final ProfileLayerMongo<X> mongoLayer = new ProfileLayerMongo<>(this);
    private final ConcurrentMap<UUID, ProfileData> data = new ConcurrentHashMap<>();
    private Jedis subscriberJedis = null;

    public ProfileCache(final Plugin plugin, final PayloadPlugin payloadPlugin, final PayloadAPI api, String name, Class<X> type) {
        super(plugin, payloadPlugin, api, name, UUID.class, type);
    }

    /**
     * Called internally by {@link PayloadCache#start()}
     */
    @Override
    protected void init() {
        // Startup!
        if (this.mode.equals(PayloadMode.NETWORK_NODE)) {
            this.handshakeManager.getTimeoutTask().start();
        }

        this.layerController.register(this.localLayer);
        this.layerController.register(this.redisLayer);
        this.layerController.register(this.mongoLayer);

        this.layerController.init();
    }

    /**
     * Called internally by {@link PayloadCache#stop()}
     */
    @Override
    protected void shutdown() {
        // close layers in order, save all objects, etc.

        for (Player player : this.getPlugin().getServer().getOnlinePlayers()) {
            X payload = this.getLocalProfile(player);
            if (payload != null) {
                payload.setOnline(false);
                payload.setLastSeenTimestamp(System.currentTimeMillis());
            }
        }
        // ^ they will be saved in the superclass's shutdown impl

        if (this.mode.equals(PayloadMode.NETWORK_NODE)) {
            this.handshakeManager.getTimeoutTask().stop();
        }
        this.layerController.shutdown();
        if (this.handshakeListener.isSubscribed()) {
            this.handshakeListener.unsubscribe();
        }
        if (this.subscriberJedis != null) {
            this.subscriberJedis.close();
        }
        this.subscriberJedis = null;
        this.data.clear();
        this.controllers.clear();
    }

    /**
     * See superclass for documentation {@link PayloadCache}
     * @param payload Payload to cache (save locally)
     */
    @Override
    public void cache(X payload) {
        if (this.hasProfileLocal(payload.getUniqueId())) {
            this.updatePayloadFromNewer(this.getLocalLayer().get(payload.getUniqueId()), payload);
            this.syncManager.updateHooks(this.getFromCache(payload.getUniqueId()));
        } else {
            this.saveToLocal(payload);
        }
    }

    /**
     * See superclass for documentation {@link PayloadCache}
     * @param payload the Payload to save
     */
    @Override
    public void saveToLocal(X payload) {
        this.localLayer.save(payload);
    }

    /**
     * Get a profile by username
     * This uses either an online matching player or the UUID cache to determine a unique ID based on the username.
     * If no unique id can be found, we will revert to the Mongo Layer
     * @param username The player's username
     * @return {@link PayloadProfile}
     */
    public X getProfileByName(String username) {
        UUID uuid = payloadPlugin.getUUIDs().get(username.toLowerCase());
        if (uuid != null) {
            return this.getProfile(uuid);
        }
        Player exact = Bukkit.getPlayerExact(username);
        if (exact != null) {
            return this.getProfile(exact);
        } else {
            // Manually get from MongoDB
            return this.mongoLayer.getByUsername(username);
        }
    }

    public X getLocalProfileByName(String username) {
        UUID uuid = PayloadPlugin.get().getUUID(username);
        if (uuid != null) {
            return this.getProfile(uuid);
        }
        Player exact = Bukkit.getPlayerExact(username);
        if (exact != null) {
            return this.getProfile(exact);
        }
        return null;
    }


    public X getProfile(UUID uuid) {
        return this.get(uuid);
    }

    public X getProfile(Player player) {
        return this.get(player.getUniqueId());
    }

    public boolean hasProfileLocal(UUID uuid) {
        return this.localLayer.has(uuid);
    }

    public boolean hasProfileLocal(Player player) {
        return this.hasProfileLocal(player.getUniqueId());
    }

    /**
     * See superclass for documentation {@link PayloadCache}
     * @param uniqueId {@link UUID} of the player to get a Profile for
     * @return {@link PayloadProfile}
     */
    @Override
    protected X get(UUID uniqueId) {
        if (this.getFailureManager().hasFailure(uniqueId)) {
            // They are attempting to be cached
            FailedPayload<X, ProfileData> failedPayload = this.getFailureManager().getFailedPayload(uniqueId);
            if (failedPayload.getTemporaryPayload() == null) {
                if (failedPayload.getPlayer() != null && failedPayload.getPlayer().isOnline()) {
                    failedPayload.setTemporaryPayload(this.instantiator.instantiate(this.createData(failedPayload.getPlayer().getName(), uniqueId, failedPayload.getPlayer().getAddress().getAddress().getHostAddress())));
                }
            }
            return failedPayload.getTemporaryPayload();
        }

        ProfileData data = this.createData(null, uniqueId, null);
        PayloadProfileController<X> controller = this.controller(data);
        controller.setLogin(false);
        return controller.cache();
    }

    public X getLocalProfile(Player player) {
        return this.localLayer.getLocalCache().get(player.getUniqueId());
    }

    public Future<X> getProfileAsync(Player player) {
        return this.pool.submit(() -> this.getProfile(player));
    }

    public Future<X> getProfileAsync(UUID uuid) {
        return this.pool.submit(() -> this.getProfile(uuid));
    }

    public Future<X> getProfileByNameAsync(String username) {
        return this.pool.submit(() -> this.getProfileByName(username));
    }

    public Set<X> getOnlineProfiles() {
        return this.localLayer.getLocalCache().values().stream()
                .filter(PayloadProfile::isPlayerOnline)
                .collect(Collectors.toSet());
    }

    /**
     * See superclass for documentation {@link PayloadCache#isCached(Object)}
     * @param key Key
     * @return
     */
    @Override
    public boolean isCached(UUID key) {
        return this.localLayer.has(key);
    }

    /**
     * See superclass for documentation {@link PayloadCache#uncache(Object)}
     * @param key The key for the object to remove (identifier)
     * @return
     */
    @Override
    public boolean uncache(UUID key) {
        if (this.getSyncMode().equals(SyncMode.CACHE_ALL) && !this.settings.isServerSpecific()) {
            if (this.getSettings().isEnableSync()) {
                this.syncManager.publishUncache(key);
            }
        }
        return this.uncacheLocal(key);
    }

    /**
     * See superclass for documentation {@link PayloadCache#uncacheLocal(Object)}
     * @param key The key for the object to remove (identifier)
     * @return
     */
    @Override
    public boolean uncacheLocal(UUID key) {
        if (this.localLayer.has(key)) {
            this.localLayer.remove(key);
            return true;
        }
        return false;
    }

    /**
     * See superclass for documentation {@link PayloadCache#getFromCache(Object)}
     * @param key The key to use to get the object
     * @return
     */
    @Override
    public X getFromCache(UUID key) {
        return this.localLayer.get(key);
    }

    /**
     * See superclass for documentation {@link PayloadCache#getFromDatabase(Object)}
     * @param key The key to use to get the object
     * @return
     */
    @Override
    public X getFromDatabase(UUID key) {
        for (PayloadLayer<UUID, X, ProfileData> layer : this.layerController.getLayers()) {
            if (layer.isDatabase()) {
                X payload = layer.get(key);
                if (payload != null) {
                    return payload;
                }
            }
        }
        return null;
    }

    /**
     * See superclass for documentation {@link PayloadCache#delete(Object)} )
     * @param key Key of payload to delete
     */
    @Override
    public void delete(UUID key) {
        for (PayloadLayer<UUID, X, ProfileData> layer : this.getLayerController().getLayers()) {
            layer.remove(key);
        }
        if (this.settings.isEnableSync()) {
            this.syncManager.publishUncache(key);
        }
    }

    /**
     * See superclass for documentation {@link PayloadCache#cacheAll()}
     */
    @Override
    public void cacheAll() {
        this.getAll().forEach(this::cache);
    }

    public ProfileData createData(String username, UUID uniqueId, String ip) {
        ProfileData data = new ProfileData(username, uniqueId, ip);
        this.data.put(uniqueId, data);
        return data;
    }

    public ProfileData getData(UUID uuid) {
        return this.data.get(uuid);
    }

    public boolean hasData(UUID uuid) {
        return this.data.containsKey(uuid);
    }

    public void removeData(UUID uuid) {
        this.data.remove(uuid);
    }

    /**
     * See superclass for documentation {@link PayloadCache#keyFromString(String)}
     * @param key String key
     * @return
     */
    @Override
    public UUID keyFromString(String key) {
        return UUID.fromString(key);
    }

    public Set<X> getAll() {
        final Set<X> all = this.localLayer.getAll().stream().filter(PayloadProfile::isOnlineThisServer).collect(Collectors.toSet());
        all.addAll(this.redisLayer.getAll().stream().filter(x -> all.stream().noneMatch(x2 -> x.getUniqueId().equals(x2.getUniqueId()))).collect(Collectors.toSet()));
        all.addAll(this.mongoLayer.getAll().stream().filter(x -> all.stream().noneMatch(x2 -> x.getUniqueId().equals(x2.getUniqueId()))).collect(Collectors.toSet()));
        return all;
    }

    /**
     * See superclass for documentation {@link PayloadCache#controller(PayloadData)}
     * @param data {@link PayloadData}
     * @return
     */
    @Override
    public PayloadProfileController<X> controller(ProfileData data) {
        if (this.controllers.containsKey(data.getUniqueId())) {
            return this.controllers.get(data.getUniqueId());
        }
        PayloadProfileController<X> controller = new PayloadProfileController<>(this, data);
        this.controllers.put(data.getUniqueId(), controller);
        return controller;
    }

    /**
     * See superclass for documentation {@link PayloadCache#saveAsync(Payload)}
     * @param payload Payload to save
     * @return
     */
    @Override
    public Future<X> saveAsync(X payload) {
        this.cache(payload);
        return this.runAsync(() -> {
            this.save(payload);
            return payload;
        });
    }

    /**
     * See superclass for documentation {@link PayloadCache#save(Payload)}
     * @param payload Payload to save
     * @return
     */
    @Override
    public boolean save(X payload) {
        this.getErrorHandler().debug(this, "Saving payload: " + payload.getIdentifier().toString());
        if (this.saveNoSync(payload)) {
            if (!payload.isSwitchingServers()) {
                if (this.settings.isEnableSync() && !this.settings.isServerSpecific()) {
                    this.syncManager.publishUpdate(payload); // Publish the update to other servers
                }
            }
            return true;
        }
        return false;
    }

    /**
     * See superclass for documentation {@link PayloadCache#saveNoSync(Payload)}
     * @param payload Payload to save
     * @return
     */
    @Override
    public boolean saveNoSync(X payload) {
        boolean x = true;
        payload.setLastInteractionTimestamp(System.currentTimeMillis());
        this.cache(payload);
        for (PayloadLayer<UUID, X, ProfileData> layer : this.layerController.getLayers()) {
            if (layer.isDatabase()) {
                if (!layer.save(payload)) {
                    x = false;
                }
            }
        }
        if (!x) {
            payload.setSaveFailed(true); // save failed
            payload.sendMessage(this.getLangController().get(PLang.SAVE_FAILED_NOTIFY_PLAYER, this.getName()));
            this.alert(PayloadPermission.ADMIN, PLang.SAVE_FAILED_NOTIFY_ADMIN, this.getName(), payload.getUsername());
            this.getErrorHandler().debug(this, "Failed to save Payload: " + payload.getUsername());
        } else {
            payload.setLastSaveTimestamp(System.currentTimeMillis());
            if (payload.isSaveFailed()) {
                // They previously had failed to save
                // but now we are successful.
                // let them know that they are free to switch servers / logout / etc. without data loss now.
                payload.sendMessage(this.getLangController().get(PLang.SAVE_SUCCESS_NOTIFY_PLAYER, this.getName()));
                this.alert(PayloadPermission.ADMIN, PLang.SAVE_SUCCESS_NOTIFY_ADMIN, this.getName(), payload.getUsername());
                this.getErrorHandler().debug(this, "Successfully saved Payload: " + payload.getUsername());
            }
            payload.setSaveFailed(false);
        }
        return x;
    }

    public boolean save(Player player) {
        X x = getProfile(player);
        if (x != null) {
            return this.save(x);
        }
        return false;
    }

    /**
     * See superclass for documentation {@link PayloadCache#saveAll()}
     * @return
     */
    @Override
    public int saveAll() {
        int failures = 0;
        for (Player p : this.getPlugin().getServer().getOnlinePlayers()) {
            X payload = this.getLocalProfile(p);
            if (payload != null) {
                payload.setLastSeenTimestamp(System.currentTimeMillis());
                payload.setLastSeenServer(PayloadAPI.get().getPayloadID());
                payload.setLastInteractionTimestamp(System.currentTimeMillis());
                if (!this.save(payload)) {
                    failures++;
                }
            } else {
                failures++;
            }
        }
        return failures;
    }

    public PayloadProfileController<X> getController(UUID uuid) {
        return this.controllers.get(uuid);
    }

    public void removeController(UUID uuid) {
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

    @Override
    public ProfileCacheSettings getSettings() {
        return this.settings;
    }

    @Override
    public long cachedObjectCount() {
        return this.localLayer.size();
    }

    @Override
    public void onRedisInitConnect() {
        // Allocate Jedis resources for Publishing and Subscribing
        if (this.payloadDatabase != null && this.payloadDatabase.getJedisPool() != null) {
            if (this.subscriberJedis == null) {
                this.getErrorHandler().debug(this, "Subscribing to pub/sub events");
                this.getPool().submit(() -> {
                    this.subscriberJedis = this.payloadDatabase.getResource();
                    this.subscriberJedis.subscribe(this.handshakeListener,
                            HandshakeEvent.PAYLOAD_NOT_CACHED_CONTINUE.getName(),
                            HandshakeEvent.REQUEST_PAYLOAD_SAVE.getName(),
                            HandshakeEvent.SAVED_PAYLOAD.getName(),
                            HandshakeEvent.SAVING_PAYLOAD.getName());
                });
            }
        }
        super.onRedisInitConnect();
    }

    /**
     * See superclass for documentation {@link PayloadCache#getCachedObjects()}
     * @return
     */
    @Override
    public Collection<X> getCachedObjects() {
        return this.localLayer.getLocalCache().values();
    }

    /**
     * See superclass for documentation {@link PayloadCache#updatePayloadID()}
     */
    @Override
    public void updatePayloadID() {
        for (X x : this.getCachedObjects()) {
            x.setPayloadId(PayloadAPI.get().getPayloadID());
            if (x.isPlayerOnline()) {
                x.setLastSeenServer(PayloadAPI.get().getPayloadID());
            }
        }
        this.pool.submit(this::saveAll);
    }
}
