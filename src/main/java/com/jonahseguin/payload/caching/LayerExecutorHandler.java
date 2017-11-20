package com.jonahseguin.payload.caching;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;

/**
 * Created by Jonah on 11/18/2017.
 * Project: Payload
 *
 * @ 8:19 PM
 */
public class LayerExecutorHandler<X extends Profile> {

    private final ProfileCache<X> profileCache;

    public LayerExecutorHandler(ProfileCache<X> profileCache) {
        this.profileCache = profileCache;
    }

    public CacheLayerExecutor<X, CachingProfile<X>, ProfilePassable> preCachingExecutor(ProfilePassable passable) {
        return new CacheLayerExecutor<>(profileCache.getLayerController().getPreCachingLayer(), passable);
    }

    public CacheLayerExecutor<X, X, ProfilePassable> creationExecutor(ProfilePassable passable) {
        return new CacheLayerExecutor<>(profileCache.getLayerController().getCreationLayer(), passable);
    }

    public CacheLayerExecutor<X, X, CachingProfile<X>> localExecutor(CachingProfile<X> cachingProfile) {
        return new CacheLayerExecutor<>(profileCache.getLayerController().getLocalLayer(), cachingProfile);
    }

    public CacheLayerExecutor<X, X, CachingProfile<X>> redisExecutor(CachingProfile<X> cachingProfile) {
        return new CacheLayerExecutor<>(profileCache.getLayerController().getRedisLayer(), cachingProfile);
    }

    public CacheLayerExecutor<X, X, CachingProfile<X>> mongoExecutor(CachingProfile<X> cachingProfile) {
        return new CacheLayerExecutor<>(profileCache.getLayerController().getMongoLayer(), cachingProfile);
    }



}
