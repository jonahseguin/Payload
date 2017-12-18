package com.jonahseguin.payload.object.layers;

import com.jonahseguin.payload.common.exception.PayloadException;
import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.type.OLayerType;
import lombok.Getter;

@Getter
public class ObjectLayerController<X extends ObjectCacheable> {

    private final PayloadObjectCache<X> cache;
    private final OLocalLayer<X> localLayer;
    private final ORedisLayer<X> redisLayer;
    private final OMongoLayer<X> mongoLayer;

    public ObjectLayerController(PayloadObjectCache<X> cache) {
        this.cache = cache;
        this.localLayer = new OLocalLayer<>(cache, cache.getSettings().getCacheDatabase());
        this.redisLayer = new ORedisLayer<>(cache, cache.getSettings().getCacheDatabase(), cache.getSettings().getType());
        this.mongoLayer = new OMongoLayer<>(cache, cache.getSettings().getCacheDatabase(), cache.getSettings().getType());
    }

    public ObjectCacheLayer<X> getLayer(OLayerType layerType) {
        if (layerType == OLayerType.LOCAL) {
            return localLayer;
        }
        else if (layerType == OLayerType.REDIS) {
            return redisLayer;
        }
        else if (layerType == OLayerType.MONGO) {
            return mongoLayer;
        }
        else {
            throw new PayloadException("Layer type is not supported: " + layerType.toString());
        }
    }

    public final boolean init() {
        boolean success = true;
        if (cache.getSettings().isUseLocal()) {
            success = localLayer.init();
        }
        if (cache.getSettings().isUseRedis()) {
            if (success && !redisLayer.init()) {
                success = false;
            }
        }
        if (cache.getSettings().isUseMongo()) {
            if (success && !mongoLayer.init()) {
                success = false;
            }
        }
        return success;
    }

    public final boolean shutdown() {
        boolean success = localLayer.shutdown();
        if (success && !redisLayer.shutdown()) {
            success = false;
        }
        if (success && !mongoLayer.shutdown()) {
            success = false;
        }
        return success;
    }

}
