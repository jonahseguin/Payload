/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jonahseguin.payload.base.PayloadCacheService;
import com.jonahseguin.payload.base.sync.SyncMode;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;

public class TestingModule extends AbstractModule {

    private final Testing testing;

    public TestingModule(Testing testing) {
        this.testing = testing;
    }

    @Override
    protected void configure() {

    }

    @Provides
    @Singleton
    ProfileCache<PayloadProfile> provideProfileCache(PayloadCacheService cacheService) {
        ProfileCache<PayloadProfile> cache = cacheService.createProfileCache("Test", PayloadProfile.class);
        cache.setSyncMode(SyncMode.CACHE_ALL);
        return cache;
    }

}
