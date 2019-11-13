/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.mode.object.settings.ObjectCacheSettings;
import com.jonahseguin.payload.mode.object.store.ObjectStoreLocal;
import com.jonahseguin.payload.mode.object.store.ObjectStoreMongo;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
@Singleton
public class PayloadObjectCache<X extends PayloadObject> extends PayloadCache<String, X, NetworkObject> implements ObjectCache<X> {

    private final ObjectCacheSettings settings = new ObjectCacheSettings();
    private final ConcurrentMap<String, PayloadObjectController<X>> controllers = new ConcurrentHashMap<>();
    private final ObjectStoreLocal<X> localStore = new ObjectStoreLocal<>(this);
    private final ObjectStoreMongo<X> mongoStore = new ObjectStoreMongo<>(this);

    public PayloadObjectCache(Injector injector, String name, Class<X> payload) {
        super(injector, name, String.class, payload, NetworkObject.class);
        setupModule();
    }

    @Override
    protected void setupModule() {
        super.injectMe();
        super.setupModule();
        injector.injectMembers(this);
    }

    @Override
    protected boolean initialize() {
        boolean success = true;
        if (settings.isUseMongo()) {
            if (!mongoStore.start()) {
                success = false;
                errorService.capture("Failed to start MongoDB store for cache " + name);
            }
        }
        if (mode.equals(PayloadMode.NETWORK_NODE)) {
            handshakeService.subscribe(new ObjectHandshake(this));
        }
        database.getMorphia().map(NetworkObject.class);
        return success;
    }

    @Override
    protected boolean terminate() {
        controllers.clear();
        return true;
    }

    @Nonnull
    @Override
    public PayloadObjectController<X> controller(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        return controllers.computeIfAbsent(key, s -> new PayloadObjectController<>(this, s));
    }

    @Override
    public boolean saveNoSync(@Nonnull X payload) {
        boolean success = true;
        if (!localStore.save(payload)) {
            success = false;
        }
        if (!mongoStore.save(payload)) {
            success = false;
        }
        if (success && mode.equals(PayloadMode.NETWORK_NODE)) {
            Optional<NetworkObject> o = networkService.get(payload);
            if (o.isPresent()) {
                NetworkObject no = o.get();
                no.markSaved();
                if (!networkService.save(no)) {
                    success = false;
                }
            }
        }
        return success;
    }

    @Nonnull
    @Override
    public PayloadStore<String, X> getDatabaseStore() {
        return mongoStore;
    }

    @Override
    public String keyToString(@Nonnull String key) {
        return key;
    }

    @Nonnull
    @Override
    public ObjectCacheSettings getSettings() {
        return settings;
    }

    @Override
    public String keyFromString(@Nonnull String key) {
        return key;
    }

    @Override
    public void cacheAll() {
        getAll().forEach(this::cache);
    }

    @Nonnull
    @Override
    public Set<X> getAll() {
        final Set<X> all = new HashSet<>(localStore.getAll());
        if (settings.isUseMongo()) {
            all.addAll(mongoStore.getAll().stream().filter(x -> all.stream().noneMatch(x2 -> x.getObjectId().equals(x2.getObjectId()))).collect(Collectors.toSet()));
        }
        return all;
    }

    @Override
    public int saveAll() {
        int failures = 0;
        for (X object : localStore.getLocalCache().values()) {
            if (!save(object)) {
                failures++;
            }
        }
        return failures;
    }

    @Override
    public boolean requireRedis() {
        return settings.isUseRedis();
    }

    @Override
    public boolean requireMongoDb() {
        return settings.isUseMongo();
    }

    @Nonnull
    @Override
    public Collection<X> getCached() {
        return localStore.getLocalCache().values();
    }

    @Override
    public void updatePayloadID() {
        getCached().forEach(o -> o.setPayloadId(api.getPayloadID()));
    }
}
