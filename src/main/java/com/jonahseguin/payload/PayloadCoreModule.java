package com.jonahseguin.payload;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
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
        bind(PCommandHandler.class).toInstance(payloadPlugin.getCommandHandler());
        bind(PayloadLocal.class).toInstance(payloadPlugin.getLocal());
    }
}
