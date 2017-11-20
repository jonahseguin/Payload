package com.jonahseguin.payload.layers;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.exception.CachingException;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.type.CacheSource;
import lombok.Getter;

/**
 * Created by Jonah on 11/18/2017.
 * Project: Payload
 *
 * @ 8:23 PM
 */
@Getter
public class LayerController<T extends Profile> {

    private final ProfileCache<T> cache;

    private final PreCachingLayer<T> preCachingLayer;
    private final UsernameUUIDLayer<T> usernameUUIDLayer;
    private final ProfileCreationLayer<T> creationLayer;
    private final LocalLayer<T> localLayer;
    private final RedisLayer<T> redisLayer;
    private final MongoLayer<T> mongoLayer;

    public LayerController(ProfileCache<T> cache) {
        this.cache = cache;

        this.preCachingLayer = new PreCachingLayer<>(cache, cache.getDatabase());
        this.usernameUUIDLayer = new UsernameUUIDLayer<>(cache, cache.getDatabase());
        this.creationLayer = new ProfileCreationLayer<>(cache, cache.getDatabase());
        this.localLayer = new LocalLayer<>(cache, cache.getDatabase());
        this.redisLayer = new RedisLayer<>(cache, cache.getDatabase(), cache.getProfileClass());
        this.mongoLayer = new MongoLayer<>(cache, cache.getDatabase(), cache.getProfileClass());
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

    public CacheLayer getLayer(CacheSource source) {
        if (source == CacheSource.PRE_CACHING) {
            return preCachingLayer;
        } else if (source == CacheSource.USERNAME_UUID) {
            return usernameUUIDLayer;
        } else if (source == CacheSource.LOCAL) {
            return localLayer;
        } else if (source == CacheSource.REDIS) {
            return redisLayer;
        } else if (source == CacheSource.MONGO) {
            return mongoLayer;
        } else {
            throw new CachingException("No layer found for source " + source.toString());
        }
    }

}
