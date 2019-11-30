/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object.store;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.object.PayloadObjectCache;
import lombok.Getter;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ObjectStoreLocal<X extends PayloadObject> extends ObjectCacheStore<X> {

    private final ConcurrentMap<String, X> localCache = new ConcurrentHashMap<>();
    private boolean running = false;

    public ObjectStoreLocal(PayloadObjectCache<X> cache) {
        super(cache);
    }

    @Override
    public Optional<X> get(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        return Optional.ofNullable(this.localCache.get(key.toLowerCase()));
    }

    public Optional<X> getByObjectID(ObjectId id) {
        return this.localCache.values().stream().filter(x -> x.getObjectId().equals(id)).findFirst();
    }

    @Override
    public boolean save(@Nonnull X payload) {
        this.localCache.put(payload.getIdentifier().toLowerCase(), payload);
        return true;
    }

    @Override
    public boolean has(@Nonnull String key) {
        return this.localCache.containsKey(key.toLowerCase());
    }

    @Override
    public boolean has(@Nonnull X payload) {
        return this.has(payload.getIdentifier().toLowerCase());
    }

    @Override
    public void remove(@Nonnull String key) {
        this.localCache.remove(key.toLowerCase());
    }

    @Override
    public void remove(@Nonnull X payload) {
        this.remove(payload.getIdentifier());
    }

    @Nonnull
    @Override
    public Collection<X> getAll() {
        return this.localCache.values();
    }

    @Override
    public int cleanup() {
        return 0;
    }

    @Override
    public long clear() {
        final int size = this.localCache.size();
        this.localCache.clear();
        return size;
    }

    @Override
    public boolean start() {
        running = true;
        return true;
    }

    @Override
    public boolean shutdown() {
        this.clear();
        running = false;
        return true;
    }

    @Nonnull
    @Override
    public String layerName() {
        return "Object Local";
    }

    @Override
    public long size() {
        return this.localCache.size();
    }

    @Override
    public boolean isDatabase() {
        return false;
    }
}
