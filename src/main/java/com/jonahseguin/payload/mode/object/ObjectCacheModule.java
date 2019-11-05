package com.jonahseguin.payload.mode.object;

import com.google.inject.Provides;
import com.jonahseguin.payload.base.CacheModule;

import javax.annotation.Nonnull;

public class ObjectCacheModule<X extends PayloadObject> extends CacheModule<String, X, NetworkObject, ObjectData> {

    private final ObjectCache<X> cache;

    ObjectCacheModule(@Nonnull ObjectCache<X> cache) {
        super(cache);
        this.cache = cache;
    }

    @Provides
    ObjectCache<X> provideObjectCache() {
        return this.cache;
    }

    @Provides
    ObjectService<X> provideObjectService() {
        return this.cache;
    }

}
