package com.jonahseguin.payload.base.handshake;

import com.google.inject.Injector;
import lombok.Getter;

@Getter
public class HandshakeContainer<H extends HandshakeController> {

    private final Class<H> type;
    private final Injector injector;
    private final H subscriberController;

    public HandshakeContainer(Class<H> type, Injector injector) {
        this.type = type;
        this.injector = injector;
        this.subscriberController = createInstance();
    }

    public H createInstance() {
        return injector.getInstance(this.type);
    }

}
