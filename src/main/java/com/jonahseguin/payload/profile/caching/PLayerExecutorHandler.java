package com.jonahseguin.payload.profile.caching;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.Profile;
import com.jonahseguin.payload.profile.profile.ProfilePassable;

/**
 * Created by Jonah on 11/18/2017.
 * Project: Payload
 *
 * @ 8:19 PM
 */
public class PLayerExecutorHandler<X extends Profile> {

    private final PayloadProfileCache<X> profileCache;

    public PLayerExecutorHandler(PayloadProfileCache<X> profileCache) {
        this.profileCache = profileCache;
    }

    public PCacheLayerExecutor<X, CachingProfile<X>, ProfilePassable> preCachingExecutor(ProfilePassable passable) {
        return new PCacheLayerExecutor<>(profileCache.getLayerController().getPreCachingLayer(), passable);
    }

    public PCacheLayerExecutor<X, X, ProfilePassable> creationExecutor(ProfilePassable passable) {
        return new PCacheLayerExecutor<>(profileCache.getLayerController().getCreationLayer(), passable);
    }

    public PCacheLayerExecutor<X, X, CachingProfile<X>> localExecutor(CachingProfile<X> cachingProfile) {
        return new PCacheLayerExecutor<>(profileCache.getLayerController().getLocalLayer(), cachingProfile);
    }

    public PCacheLayerExecutor<X, X, CachingProfile<X>> redisExecutor(CachingProfile<X> cachingProfile) {
        return new PCacheLayerExecutor<>(profileCache.getLayerController().getRedisLayer(), cachingProfile);
    }

    public PCacheLayerExecutor<X, X, CachingProfile<X>> mongoExecutor(CachingProfile<X> cachingProfile) {
        return new PCacheLayerExecutor<>(profileCache.getLayerController().getMongoLayer(), cachingProfile);
    }



}
