package com.jonahseguin.payload.object.caching;

import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.obj.ObjectCacheable;

public class ObjectLayerExecutorHandler<X extends ObjectCacheable> {

    private final PayloadObjectCache<X> cache;

    public ObjectLayerExecutorHandler(PayloadObjectCache<X> cache) {
        this.cache = cache;
    }

    public ObjectLayerExecutor<X> localExecutor(String id) {
        return new ObjectLayerExecutor<>(cache.getLayerController().getLocalLayer(), id);
    }

    public ObjectLayerExecutor<X> redisExecutor(String id) {
        return new ObjectLayerExecutor<>(cache.getLayerController().getRedisLayer(), id);
    }

    public ObjectLayerExecutor<X> mongoExecutor(String id) {
        return new ObjectLayerExecutor<>(cache.getLayerController().getMongoLayer(), id);
    }

}
