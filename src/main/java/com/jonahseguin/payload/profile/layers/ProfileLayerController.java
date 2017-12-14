package com.jonahseguin.payload.profile.layers;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.common.exception.CachingException;
import com.jonahseguin.payload.profile.profile.Profile;
import com.jonahseguin.payload.profile.type.PCacheSource;
import lombok.Getter;

/**
 * Created by Jonah on 11/18/2017.
 * Project: Payload
 *
 * @ 8:23 PM
 */
@Getter
public class ProfileLayerController<T extends Profile> {

    private final PayloadProfileCache<T> cache;

    private final PreCachingLayer<T> preCachingLayer;
    private final PUsernameUUIDLayer<T> usernameUUIDLayer;
    private final ProfileCreationLayer<T> creationLayer;
    private final PLocalLayer<T> localLayer;
    private final PRedisLayer<T> redisLayer;
    private final PMongoLayer<T> mongoLayer;

    public ProfileLayerController(PayloadProfileCache<T> cache) {
        this.cache = cache;

        this.preCachingLayer = new PreCachingLayer<>(cache, cache.getDatabase());
        this.usernameUUIDLayer = new PUsernameUUIDLayer<>(cache, cache.getDatabase());
        this.creationLayer = new ProfileCreationLayer<>(cache, cache.getDatabase());
        this.localLayer = new PLocalLayer<>(cache, cache.getDatabase());
        this.redisLayer = new PRedisLayer<>(cache, cache.getDatabase(), cache.getProfileClass());
        this.mongoLayer = new PMongoLayer<>(cache, cache.getDatabase(), cache.getProfileClass());
    }

    public final boolean init() {
        boolean success = true;
        if (!preCachingLayer.init()) {
            success = false;
        }
        if (success && !usernameUUIDLayer.init()) {
            success = false;
        }
        if (success && !localLayer.init()) {
            success = false;
        }
        if (success && !redisLayer.init()) {
            success = false;
        }
        if (success && !mongoLayer.init()) {
            success = false;
        }
        return success;
    }

    public final boolean shutdown() {
        boolean success = true;
        if (!preCachingLayer.shutdown()) {
            success = false;
        }
        if (!usernameUUIDLayer.shutdown()) {
            success = false;
        }
        if (!localLayer.shutdown()) {
            success = false;
        }
        if (!redisLayer.shutdown()) {
            success = false;
        }
        if (!mongoLayer.shutdown()) {
            success = false;
        }
        return success;
    }

    public ProfileCacheLayer getLayer(PCacheSource source) {
        if (source == PCacheSource.PRE_CACHING) {
            return preCachingLayer;
        } else if (source == PCacheSource.USERNAME_UUID) {
            return usernameUUIDLayer;
        } else if (source == PCacheSource.LOCAL) {
            return localLayer;
        } else if (source == PCacheSource.REDIS) {
            return redisLayer;
        } else if (source == PCacheSource.MONGO) {
            return mongoLayer;
        } else {
            throw new CachingException("No layer found for source " + source.toString());
        }
    }

}
