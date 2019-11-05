/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jonahseguin.payload.base.CacheService;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileService;

public class TestingModule extends AbstractModule {

    private final Testing testing;

    TestingModule(Testing testing) {
        this.testing = testing;
    }

    @Override
    protected void configure() {
        bind(Testing.class).toInstance(testing);
    }

    @Provides @Singleton
    ProfileService<PayloadProfile> provideProfileCache(CacheService cacheService) {
        ProfileService<PayloadProfile> cache = cacheService.createProfileCache("Test", PayloadProfile.class);
        cache.setMode(PayloadMode.NETWORK_NODE);
        cache.getSettings().setEnableSync(true);
        return cache;
    }

}
