/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.jonahseguin.payload.base.CacheService;
import com.jonahseguin.payload.base.DatabaseCacheService;
import com.jonahseguin.payload.base.lang.PLangService;
import com.jonahseguin.payload.base.lifecycle.LifecycleService;
import com.jonahseguin.payload.base.lifecycle.PluginLifecycleService;
import com.jonahseguin.payload.base.uuid.UUIDService;
import com.jonahseguin.payload.database.DatabaseModule;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;

public class PayloadModule extends AbstractModule {

    private final PayloadPlugin payloadPlugin;
    private final JavaPlugin plugin;
    private final DatabaseModule databaseModule;

    PayloadModule(@Nonnull JavaPlugin plugin, @Nonnull DatabaseModule databaseModule) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(databaseModule);
        this.plugin = plugin;
        this.databaseModule = databaseModule;
        this.payloadPlugin = PayloadPlugin.getPlugin();
    }

    @Override
    protected void configure() {
        bind(PayloadAPI.class).toInstance(payloadPlugin.getApi());
        bind(PayloadPlugin.class).toInstance(payloadPlugin);
        bind(PayloadLocal.class).toInstance(payloadPlugin.getLocal());
        bind(PLangService.class).toInstance(payloadPlugin.getLang());

        bind(Plugin.class).toInstance(plugin);
        bind(JavaPlugin.class).toInstance(plugin);

        install(databaseModule);

        bind(UUIDService.class);
        bind(CacheService.class).to(DatabaseCacheService.class).in(Singleton.class);
        bind(LifecycleService.class).to(PluginLifecycleService.class).in(Singleton.class);
    }

}
