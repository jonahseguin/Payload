package com.jonahseguin.payload.base.handshake;

import com.google.inject.AbstractModule;

public class HandshakeModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(HandshakeService.class).to(PayloadHandshakeService.class);
    }
}
