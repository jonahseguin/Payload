/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.ObjectCacheModule;
import com.jonahseguin.payload.mode.object.ObjectService;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileCacheModule;
import com.jonahseguin.payload.mode.profile.ProfileService;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class DatabaseCacheService implements CacheService {

    private final Plugin plugin;
    private final DatabaseService database;
    private final PayloadPlugin payloadPlugin;
    private final PayloadAPI api;
    private final Injector injector;
    private boolean running = false;

    @Inject
    public DatabaseCacheService(Plugin plugin, DatabaseService database, PayloadPlugin payloadPlugin, PayloadAPI api, Injector injector) {
        this.plugin = plugin;
        this.database = database;
        this.payloadPlugin = payloadPlugin;
        this.api = api;
        this.injector = injector;
    }

    @Override
    public <X extends PayloadProfile> ProfileService<X> createProfileCache(@Nonnull String name, @Nonnull Class<X> type) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(type);
        Injector injector = this.injector.createChildInjector(new ProfileCacheModule<>(type, name));
        ProfileCache<X> cache = injector.getInstance(Key.get(new TypeLiteral<ProfileCache<X>>() {
        }));
        setup(cache, type);
        return cache;
    }

    @Override
    public <X extends PayloadObject> ObjectService<X> createObjectCache(@Nonnull String name, @Nonnull Class<X> type) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(type);
        Injector injector = this.injector.createChildInjector(new ObjectCacheModule<>(type, name));
        ObjectCache<X> cache = injector.getInstance(Key.get(new TypeLiteral<ObjectCache<X>>() {
        }));
        setup(cache, type);
        return cache;
    }

    private void setup(@Nonnull PayloadCache cache, Class type) {
        Preconditions.checkNotNull(cache);
        database.hook(cache);
        api.saveCache(cache);

        database.getMorphia().map(type);
        database.getDatastore().ensureIndexes();
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
