package com.jonahseguin.payload.caching;

import com.jonahseguin.payload.layers.CacheLayer;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;

/**
 * Created by Jonah on 11/18/2017.
 * Project: Payload
 *
 * @ 8:10 PM
 */
public class CacheLayerExecutor<P extends Profile, T extends ProfilePassable, Passable extends ProfilePassable> {

    private final CacheLayer<P, T, Passable> cacheLayer;
    private final Passable passable;
    private boolean errors = false;
    private boolean success = false;
    private T provided = null;

    public CacheLayerExecutor(CacheLayer<P, T, Passable> cacheLayer, Passable passable) {
        this.cacheLayer = cacheLayer;
        this.passable = passable;
    }

    public LayerResult<T> execute() {
        try {
            T ret = cacheLayer.provide(passable);
            if (ret != null) {
                provided = ret;
                success = true;
            }
            else {
                success = false;
                cacheLayer.debug("The provided object from cache layer " + cacheLayer.source().toString() + " was null for player " + passable.getName());
            }
        }
        catch (Exception ex) {
            success = false;
            errors = true;
            cacheLayer.error(ex, "An error occurred while executing the cache layer " + cacheLayer.source().toString() + " for player " + passable.getName());
        }
        return new LayerResult<>(success, errors, provided);
    }

}
