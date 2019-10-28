/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jonahseguin.payload.base.data.PayloadLocal;
import com.jonahseguin.payload.command.PCommandHandler;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.service.PayloadCacheService;
import com.jonahseguin.payload.service.PayloadDatabaseCacheService;
import org.bukkit.plugin.Plugin;

public class PayloadModule extends AbstractModule {

    private final PayloadPlugin payloadPlugin;
    private final Plugin plugin;

    PayloadModule(PayloadPlugin payloadPlugin, Plugin plugin) {
        this.payloadPlugin = payloadPlugin;
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(PayloadPlugin.class).toInstance(this.payloadPlugin);
        bind(Plugin.class).toInstance(this.plugin);
        bind(PayloadAPI.class);
        bind(PCommandHandler.class);
        bind(PayloadLocal.class);
        bind(PayloadCacheService.class).to(PayloadDatabaseCacheService.class);
    }

    @Provides @Singleton PayloadDatabase provideDatabase() {
        return PayloadDatabase.fromConfigFile(plugin, "database.yml", "Database", payloadPlugin.getPayloadClassLoader());
    }


}
