/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.CacheModule;
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
public class ObjectCache<X extends PayloadObject> extends PayloadCache<String, X, NetworkObject, ObjectData> implements ObjectService<X> {

    private final ObjectCacheSettings settings = new ObjectCacheSettings();
    private final ConcurrentMap<String, PayloadObjectController<X>> controllers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ObjectData> data = new ConcurrentHashMap<>();
    private final ObjectStoreLocal<X> localStore = new ObjectStoreLocal<>(this);
    private final ObjectStoreMongo<X> mongoStore = new ObjectStoreMongo<>(this);
    private final CacheModule<String, X, NetworkObject, ObjectData> module = new ObjectCacheModule<>(this);

    public ObjectCache(@Nonnull Injector injector, @Nonnull String name, @Nonnull Class<X> payloadClass) {
        super(injector, name, String.class, payloadClass);
        this.injector.injectMembers(this);
    }

    @Nonnull
    @Override
    protected CacheModule<String, X, NetworkObject, ObjectData> module() {
        return module;
    }

    @Override
    protected boolean initialize() {
        boolean success = true;
        if (settings.isUseMongo()) {
            if (!mongoStore.start()) {
                success = false;
            }
        }
        if (mode.equals(PayloadMode.NETWORK_NODE)) {
            handshakeService.subscribe(Key.get(new TypeLiteral<ObjectHandshake<X>>() {
            }));
        }
        return success;
    }

    @Override
    protected boolean terminate() {
        data.clear();
        controllers.clear();
        return true;
    }

    @Override
    public PayloadObjectController<X> controller(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        if (controllers.containsKey(key)) {
            return controllers.get(key);
        }
        ObjectData data = createData(key);
        PayloadObjectController<X> controller = new PayloadObjectController<>(this, data);
        controllers.put(key, controller);
        return controller;
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
    public PayloadStore<String, X, ObjectData> getDatabaseStore() {
        return mongoStore;
    }

    @Override
    public String keyToString(@Nonnull String key) {
        return key;
    }

    @Override
    public PayloadObjectController<X> controller(@Nonnull ObjectData data) {
        Preconditions.checkNotNull(data);
        return controller(data.getIdentifier());
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
    public long cachedObjectCount() {
        return localStore.size();
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

    public ObjectData createData(String identifier) {
        if (data.containsKey(identifier)) {
            return data.get(identifier);
        }
        ObjectData data = new ObjectData(identifier);
        this.data.put(identifier, data);
        return data;
    }

    public ObjectData getData(String identifier) {
        return data.getOrDefault(identifier, null);
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
