package com.jonahseguin.payload.mode.object.store;

import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.ObjectData;
import com.jonahseguin.payload.mode.object.PayloadObject;
import lombok.Getter;

@Getter
public abstract class ObjectCacheStore<X extends PayloadObject> implements PayloadStore<String, X, ObjectData> {

    protected final ObjectCache<X> cache;

    public ObjectCacheStore(ObjectCache<X> objectCache) {
        this.cache = objectCache;
    }
}
