package com.jonahseguin.payload.mode.object.layer;

import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.ObjectData;
import lombok.Getter;

@Getter
public abstract class ObjectCacheLayer<X extends Payload> implements PayloadLayer<String, X, ObjectData> {

    protected final ObjectCache<X> cache;

    public ObjectCacheLayer(ObjectCache<X> objectCache) {
        this.cache = objectCache;
    }
}
