/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.jonahseguin.payload.base.CacheModule;
import com.jonahseguin.payload.base.data.PayloadLocal;
import com.jonahseguin.payload.base.lang.PayloadLangController;
import com.jonahseguin.payload.base.uuid.UUIDService;
import com.jonahseguin.payload.command.PCommandHandler;
import com.jonahseguin.payload.database.DatabaseModule;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class PayloadModule extends AbstractModule {

    private final PayloadPlugin payloadPlugin;
    private final PayloadAPI api;
    private final Plugin plugin;

    PayloadModule(@Nonnull PayloadPlugin payloadPlugin, @Nonnull Plugin plugin) {
        Preconditions.checkNotNull(payloadPlugin);
        Preconditions.checkNotNull(plugin);
        this.payloadPlugin = payloadPlugin;
        this.api = payloadPlugin.getApi();
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(PayloadPlugin.class).toInstance(payloadPlugin);
        bind(PayloadLangController.class).toInstance(payloadPlugin.getLangController());
        bind(Plugin.class).toInstance(plugin);
        bind(PayloadAPI.class).toInstance(api);
        bind(PCommandHandler.class).toInstance(payloadPlugin.getCommandHandler());
        bind(PayloadLocal.class).toInstance(payloadPlugin.getLocal());
        bind(UUIDService.class);

        install(new DatabaseModule(api, payloadPlugin, plugin));
        install(new CacheModule());
    }

}
