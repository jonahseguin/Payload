package com.jonahseguin.payload.object.caching;

import com.jonahseguin.payload.common.exception.CachingException;
import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.type.OLayerType;
import lombok.Getter;

@Getter
public class ObjectCachingController<X extends ObjectCacheable> {

    private final PayloadObjectCache<X> cache;
    private final String id;
    private X loaded = null;
    private OLayerType loadedFrom = null;

    public ObjectCachingController(PayloadObjectCache<X> cache, String id) {
        this.cache = cache;
        this.id = id;
    }

    public X cache() {
        loaded = tryLocal();
        loadedFrom = OLayerType.LOCAL;
        if (loaded == null) {
            loaded = tryRedis();
            loadedFrom = OLayerType.REDIS;
            if (loaded == null) {
                loaded = tryMongo();
                loadedFrom = OLayerType.MONGO;
                if (loaded == null) {
                    if (cache.getSettings().isCreateOnNull()) {
                        loaded = cache.getSettings().getObjectInstantiator().instantiate(id);
                        loadedFrom = null; // Created
                    }
                }
            }
        }

        if (loaded != null) {
            if (cache.getSettings().isSaveAfterLoad()) {
                boolean successSaving = saveObjectAfterLoad(loaded, loadedFrom);
                // Do something with the result of the success?
            }
            return loaded;
        }
        else {

            return null;
        }
    }

    private boolean saveObjectAfterLoad(X obj, OLayerType except) {
        // Save profile to all layers /except/ the layer it was provided from
        boolean success = true;
        if (except == null || !except.equals(OLayerType.LOCAL)) {
            boolean local = cache.getLayerController().getLocalLayer().save(obj.getIdentifier(), obj);
            success = local;
            if (!local) {
                cache.getDebugger().error(new CachingException("Local layer failed to save when saving Object after load for ID " + obj.getIdentifier()));
            }
        }
        if (except == null || !except.equals(OLayerType.REDIS)) {
            boolean redis = cache.getLayerController().getRedisLayer().save(obj.getIdentifier(), obj);
            if (success) {
                success = redis;
            }
            if (!redis) {
                cache.getDebugger().error(new CachingException("Redis layer failed to save when saving Object after load for ID " + obj.getIdentifier()));
            }
        }
        if (except == null || !except.equals(OLayerType.MONGO)) {
            boolean mongo = cache.getLayerController().getMongoLayer().save(obj.getIdentifier(), obj);
            if (success) {
                success = mongo;
            }
            if (!mongo) {
                cache.getDebugger().error(new CachingException("Mongo layer failed to save when saving Object after load for ID " + obj.getIdentifier()));
            }
        }
        return success;
    }

    private X tryLocal() {
        ObjectLayerResult<X> result = cache.getExecutorHandler().localExecutor(id).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

    private X tryRedis() {
        if (cache.getSettings().isUseRedis()) {
            ObjectLayerResult<X> result = cache.getExecutorHandler().redisExecutor(id).execute();
            if (result.isSuccess()) {
                return result.getResult();
            }
        }
        return null;
    }

    private X tryMongo() {
        if (cache.getSettings().isUseMongo()) {
            ObjectLayerResult<X> result = cache.getExecutorHandler().mongoExecutor(id).execute();
            if (result.isSuccess()) {
                return result.getResult();
            }
        }
        return null;
    }

}