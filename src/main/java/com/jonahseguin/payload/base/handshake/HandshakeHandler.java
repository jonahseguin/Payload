package com.jonahseguin.payload.base.handshake;

import com.jonahseguin.payload.base.PayloadCallback;

public class HandshakeHandler<H extends HandshakeController> {

    final HandshakeData data;
    PayloadCallback<H> callback = object -> {
    };

    public HandshakeHandler(HandshakeData data) {
        this.data = data;
    }

    public HandshakeHandler<H> afterReply(PayloadCallback<H> callback) {
        this.callback = callback;
        return this;
    }

}
