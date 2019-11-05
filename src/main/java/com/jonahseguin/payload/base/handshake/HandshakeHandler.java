package com.jonahseguin.payload.base.handshake;

import com.jonahseguin.payload.base.PayloadCallback;
import lombok.Getter;

import java.util.Optional;

@Getter
public class HandshakeHandler<H extends Handshake> {

    private volatile H controller = null;
    final HandshakeData data;
    PayloadCallback<H> callback = object -> {
    };
    private boolean timedOut = false;

    public HandshakeHandler(HandshakeData data) {
        this.data = data;
    }

    void call(H controller) {
        this.controller = controller;
        if (this.callback != null) {
            this.callback.callback(controller);
        }
    }

    public HandshakeHandler<H> afterReply(PayloadCallback<H> callback) {
        this.callback = callback;
        return this;
    }

    public synchronized Optional<H> waitForReply(int maxSeconds) {
        double waited = 0;
        while (controller == null) {
            if (waited >= maxSeconds) {
                timedOut = true;
                break;
            }
            try {
                Thread.sleep(100);
                waited += 0.1;
            } catch (InterruptedException ex) {
                this.controller.getDatabase().getErrorService().capture(ex, "Interrupted while waiting for reply in handshake handler");
            }
        }
        return Optional.ofNullable(controller);
    }

}
