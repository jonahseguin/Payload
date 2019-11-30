/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.handshake;

import com.google.common.base.Preconditions;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class HandshakeContainer {

    private final Handshake subscriberController;

    public HandshakeContainer(@Nonnull Handshake subscriber) {
        Preconditions.checkNotNull(subscriber);
        this.subscriberController = subscriber;
    }

    public Handshake createInstance() {
        return subscriberController.create();
    }

}
