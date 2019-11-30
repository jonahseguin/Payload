/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object.store;

import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.object.PayloadObjectCache;
import lombok.Getter;

@Getter
public abstract class ObjectCacheStore<X extends PayloadObject> implements PayloadStore<String, X> {

    protected final PayloadObjectCache<X> cache;

    public ObjectCacheStore(PayloadObjectCache<X> objectCache) {
        this.cache = objectCache;
    }
}
