package com.jonahseguin.payload.object.caching;

import com.jonahseguin.payload.object.layers.ObjectCacheLayer;
import com.jonahseguin.payload.object.obj.ObjectCacheable;

public class ObjectLayerExecutor<X extends ObjectCacheable> {

    private final ObjectCacheLayer<X> cacheLayer;
    private final String id;
    private boolean errors = false;
    private boolean success = false;
    private X provided = null;

    public ObjectLayerExecutor(ObjectCacheLayer<X> cacheLayer, String id) {
        this.cacheLayer = cacheLayer;
        this.id = id;
    }

    public ObjectLayerResult<X> execute() {
        try {
            X ret = cacheLayer.provide(this.id);
            if (ret != null) {
                provided = ret;
                success = true;
            }
            else {
                success = false;
                cacheLayer.debug("The provided object in object cache from layer " + cacheLayer.source().toString() + " was null for ID " + this.id);
            }
        }
        catch (Exception ex) {
            errors = true;
            success = false;
            cacheLayer.error(ex, "An error occurred while executing in object cache for layer " + cacheLayer.source().toString() + " for ID " + this.id);
        }
        return new ObjectLayerResult<>(success, errors, provided);
    }

}
