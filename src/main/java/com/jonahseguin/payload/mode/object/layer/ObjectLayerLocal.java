/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object.layer;

import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.ObjectData;
import com.jonahseguin.payload.mode.object.PayloadObject;
import lombok.Getter;
import org.bson.types.ObjectId;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ObjectLayerLocal<X extends PayloadObject> extends ObjectCacheLayer<X> {

    private final ConcurrentMap<String, X> localCache = new ConcurrentHashMap<>();

    public ObjectLayerLocal(ObjectCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(String key) {
        return this.localCache.get(key.toLowerCase());
    }

    @Override
    public X get(ObjectData data) {
        return this.get(data.getIdentifier().toLowerCase());
    }

    public X getByObjectID(ObjectId id) {
        return this.localCache.values().stream().filter(x -> x.getObjectId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public boolean save(X payload) {
        this.localCache.put(payload.getIdentifier().toLowerCase(), payload);
        return true;
    }

    @Override
    public boolean has(String key) {
        return this.localCache.containsKey(key.toLowerCase());
    }

    @Override
    public boolean has(ObjectData data) {
        return this.has(data.getIdentifier().toLowerCase());
    }

    @Override
    public boolean has(X payload) {
        return this.has(payload.getIdentifier().toLowerCase());
    }

    @Override
    public void remove(String key) {
        this.localCache.remove(key.toLowerCase());
    }

    @Override
    public void remove(ObjectData data) {
        this.remove(data.getIdentifier().toLowerCase());
    }

    @Override
    public void remove(X payload) {
        this.remove(payload.getIdentifier().toLowerCase());
    }

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
    public void init() {

    }

    @Override
    public void shutdown() {
        this.clear();
    }

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
