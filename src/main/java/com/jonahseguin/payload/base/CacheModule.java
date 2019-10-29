/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

public class CacheModule<K, X extends Payload<K>, D extends PayloadData> extends AbstractModule {

    private final PayloadCache<K, X, D> cache;

    public CacheModule(PayloadCache<K, X, D> cache) {
        this.cache = cache;
    }

    @Override
    protected void configure() {
        //bind(PayloadCacheService.class).to(PayloadDatabaseCacheService.class);
    }

    @Provides
    PayloadCache<K, X, D> provideCache() {
        return cache;
    }

}
