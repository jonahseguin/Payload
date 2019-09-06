package com.jonahseguin.payload.mode.object.layer;

import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.ObjectData;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ObjectLayerLocal<X extends Payload> extends ObjectCacheLayer<X> {

    private final ConcurrentMap<String, X> localCache = new ConcurrentHashMap<>();

    public ObjectLayerLocal(ObjectCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(String key) throws PayloadLayerCannotProvideException {
        if (!this.has(key)) {
            throw new PayloadLayerCannotProvideException("Cannot provide in local layer for object identifier: " + key, this.cache);
        }
        return this.localCache.get(key);
    }

    @Override
    public X get(ObjectData data) throws PayloadLayerCannotProvideException {
        return this.get(data.getIdentifier());
    }

    @Override
    public boolean save(X payload) {
        this.localCache.put(payload.getIdentifier(), payload);
        return true;
    }

    @Override
    public boolean has(String key) {
        return this.localCache.containsKey(key);
    }

    @Override
    public boolean has(ObjectData data) {
        return this.has(data.getIdentifier());
    }

    @Override
    public boolean has(X payload) {
        return this.has(payload.getIdentifier());
    }

    @Override
    public void remove(String key) {
        this.localCache.remove(key);
    }

    @Override
    public void remove(ObjectData data) {
        this.remove(data.getIdentifier());
    }

    @Override
    public void remove(X payload) {
        this.remove(payload.getIdentifier());
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
        return null;
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
