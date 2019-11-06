/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.jonahseguin.payload.base.CacheService;
import com.jonahseguin.payload.base.DatabaseCacheService;
import com.jonahseguin.payload.base.lifecycle.LifecycleService;
import com.jonahseguin.payload.base.lifecycle.PluginLifecycleService;
import com.jonahseguin.payload.base.uuid.UUIDService;
import com.jonahseguin.payload.database.DatabaseModule;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class PayloadModule extends AbstractModule {

    private final Plugin plugin;
    private final DatabaseModule databaseModule;

    PayloadModule(@Nonnull Plugin plugin, @Nonnull DatabaseModule databaseModule) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(databaseModule);
        this.plugin = plugin;
        this.databaseModule = databaseModule;
    }

    @Override
    protected void configure() {
        bind(Plugin.class).toInstance(plugin);
        install(new PayloadCoreModule(PayloadPlugin.getPlugin(), PayloadPlugin.getPlugin().getApi()));
        install(databaseModule);
        bind(UUIDService.class);
        bind(CacheService.class).to(DatabaseCacheService.class);
        bind(LifecycleService.class).to(PluginLifecycleService.class);
    }

}
