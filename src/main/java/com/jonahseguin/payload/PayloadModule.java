/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.jonahseguin.payload.base.CacheModule;
import com.jonahseguin.payload.base.data.PayloadLocal;
import com.jonahseguin.payload.base.lang.PayloadLangController;
import com.jonahseguin.payload.base.uuid.UUIDService;
import com.jonahseguin.payload.command.PCommandHandler;
import com.jonahseguin.payload.database.DatabaseModule;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.server.ServerManager;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class PayloadModule extends AbstractModule {

    private final PayloadPlugin payloadPlugin;
    private final Plugin plugin;

    public PayloadModule(@Nonnull PayloadPlugin payloadPlugin, @Nonnull Plugin plugin) {
        Preconditions.checkNotNull(payloadPlugin);
        Preconditions.checkNotNull(plugin);
        this.payloadPlugin = payloadPlugin;
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(PayloadPlugin.class).toInstance(payloadPlugin);
        bind(PayloadLangController.class).toInstance(payloadPlugin.getLangController());
        bind(Plugin.class).toInstance(plugin);
        bind(PayloadAPI.class).toInstance(payloadPlugin.getApi());
        bind(PCommandHandler.class).toInstance(payloadPlugin.getCommandHandler());
        bind(PayloadLocal.class).toInstance(payloadPlugin.getLocal());
        bind(UUIDService.class);

        install(new DatabaseModule(payloadPlugin.getApi(), payloadPlugin, plugin));
        install(new CacheModule());
    }

    @Provides
    ServerManager provideServerManager(PayloadDatabase database) {
        return database.getServerManager();
    }

}
