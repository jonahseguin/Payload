/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.plugin.Plugin;

public class PayloadDatabaseCacheService implements PayloadCacheService {

    private final Plugin plugin;
    private final PayloadDatabase database;
    private final PayloadPlugin payloadPlugin;
    private final PayloadAPI api;

    @Inject
    public PayloadDatabaseCacheService(Plugin plugin, PayloadDatabase database, PayloadPlugin payloadPlugin, PayloadAPI api) {
        this.plugin = plugin;
        this.database = database;
        this.payloadPlugin = payloadPlugin;
        this.api = api;
    }

    @Override
    public <X extends PayloadProfile> ProfileCache<X> createProfileCache(String name, Class<X> type) {
        ProfileCache<X> cache = new ProfileCache<>(plugin, payloadPlugin, api, name, type);
        database.hookCache(cache);
        api.saveCache(cache);

        database.getMorphia().map(type);
        database.getDatastore().ensureIndexes();
        return cache;
    }

    @Override
    public <X extends PayloadObject> ObjectCache<X> createObjectCache(String name, Class<X> type) {
        ObjectCache<X> cache = new ObjectCache<>(plugin, payloadPlugin, api, name, type);
        database.hookCache(cache);
        api.saveCache(cache);

        database.getMorphia().map(type);
        database.getDatastore().ensureIndexes();
        return cache;
    }
}
