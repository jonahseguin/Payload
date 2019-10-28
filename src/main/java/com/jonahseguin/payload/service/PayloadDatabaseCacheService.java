/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.service;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.plugin.Plugin;

public class PayloadDatabaseCacheService implements PayloadCacheService {

    private final PayloadPlugin payloadPlugin;
    private final Plugin plugin;
    private final PayloadAPI api;
    private final PayloadDatabase database;

    @Inject
    public PayloadDatabaseCacheService(PayloadPlugin payloadPlugin, Plugin plugin, PayloadAPI api, PayloadDatabase database) {
        this.payloadPlugin = payloadPlugin;
        this.plugin = plugin;
        this.api = api;
        this.database = database;
    }

    @Override
    public <X extends PayloadProfile> ProfileCache<X> createProfileCache(String name, Class<X> type) {
        ProfileCache<X> cache = new ProfileCache<>(plugin, api, payloadPlugin, name, type);
        api.saveCache(cache);
        return cache;
    }

    @Override
    public <X extends PayloadObject> ObjectCache<X> createObjectCache(String name, Class<X> type) {
        ObjectCache<X> cache = new ObjectCache<>(plugin, api, payloadPlugin, name, type);
        api.saveCache(cache);
        return cache;
    }
}
