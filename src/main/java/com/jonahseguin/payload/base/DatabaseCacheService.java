/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.type.PayloadInstantiator;
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.object.PayloadObjectCache;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileCache;

import javax.annotation.Nonnull;
import java.util.UUID;

public class DatabaseCacheService implements CacheService {

    private final DatabaseService database;
    private final PayloadAPI api;
    private final Injector injector;
    private boolean running = false;

    @Inject
    public DatabaseCacheService(DatabaseService database, PayloadAPI api, Injector injector) {
        this.database = database;
        this.api = api;
        this.injector = injector;
    }

    @Override
    public <X extends PayloadProfile> ProfileCache<X> createProfileCache(@Nonnull String name, @Nonnull Class<X> type, @Nonnull PayloadInstantiator<UUID, X> instantiator) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(instantiator);
        ProfileCache<X> cache = new PayloadProfileCache<>(injector, instantiator, name, type);
        setup(cache, type);
        return cache;
    }

    @Override
    public <X extends PayloadObject> ObjectCache<X> createObjectCache(@Nonnull String name, @Nonnull Class<X> type, @Nonnull PayloadInstantiator<String, X> instantiator) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(instantiator);
        ObjectCache<X> cache = new PayloadObjectCache<>(injector, instantiator, name, type);
        setup(cache, type);
        return cache;
    }

    private void setup(@Nonnull Cache cache, Class type) {
        Preconditions.checkNotNull(cache);
        database.hook(cache);
        api.saveCache(cache);

        database.getMorphia().map(type);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean start() {
        running = true;
        return true;
    }

    @Override
    public boolean shutdown() {
        running = false;
        return true;
    }
}
