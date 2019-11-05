/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.jonahseguin.payload.base.lang.LangService;
import com.jonahseguin.payload.base.lang.PayloadLangService;
import com.jonahseguin.payload.command.PCommandHandler;

import javax.annotation.Nonnull;

public class PayloadCoreModule extends AbstractModule {

    private final PayloadPlugin payloadPlugin;
    private final PayloadAPI api;

    PayloadCoreModule(@Nonnull PayloadPlugin payloadPlugin, @Nonnull PayloadAPI api) {
        Preconditions.checkNotNull(payloadPlugin);
        Preconditions.checkNotNull(api);
        this.payloadPlugin = payloadPlugin;
        this.api = api;
    }

    @Override
    protected void configure() {
        bind(PayloadPlugin.class).toInstance(payloadPlugin);
        bind(PayloadAPI.class).toInstance(api);
        bind(PayloadLocal.class).toInstance(payloadPlugin.getLocal());
        bind(LangService.class).to(PayloadLangService.class);
        bind(PCommandHandler.class);
    }
}
