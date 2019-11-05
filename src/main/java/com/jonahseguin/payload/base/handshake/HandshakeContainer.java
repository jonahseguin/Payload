/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.handshake;

import com.google.inject.Injector;
import com.google.inject.Key;
import lombok.Getter;

@Getter
public class HandshakeContainer<H extends Handshake> {

    private final Key<H> type;
    private final Injector injector;
    private final H subscriberController;

    public HandshakeContainer(Key<H> type, Injector injector) {
        this.type = type;
        this.injector = injector;
        this.subscriberController = createInstance();
    }

    public H createInstance() {
        return injector.getInstance(this.type);
    }

}
