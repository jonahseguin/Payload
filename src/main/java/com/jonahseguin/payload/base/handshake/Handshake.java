/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.handshake;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jonahseguin.payload.database.DatabaseService;
import lombok.Getter;
import redis.clients.jedis.Jedis;

import javax.annotation.Nonnull;

@Getter
public abstract class Handshake {

    protected final Injector injector;
    @Inject
    protected HandshakeService handshakeService;
    @Inject
    protected DatabaseService database;
    protected HandshakeListener listener = null;
    protected HandshakeHandler handler;
    protected Jedis subscriber = null;

    @Inject
    public Handshake(Injector injector) {
        this.injector = injector;
        injector.injectMembers(this);
    }


    public abstract String channelPublish();

    public abstract String channelReply();

    public abstract void load(@Nonnull HandshakeData data);

    public abstract void write(@Nonnull HandshakeData data);

    /**
     * Called when receiving this handshake from another server, before the reply is sent.  Async.
     */
    public abstract void receive();

    public abstract boolean shouldAccept();

    public boolean shouldReply() {
        return true;
    }

    void setHandler(HandshakeHandler handshakeHandler) {
        this.handler = handshakeHandler;
    }

    @SuppressWarnings("unchecked")
    void executeHandler() {
        if (handler != null) {
            handler.call(this);
        }
    }

    public void listen() {
        if (listener == null) {
            listener = new HandshakeListener(handshakeService, this);
        }
        if (subscriber == null) {
            subscriber = database.getJedisResource();
        }
        if (!listener.isSubscribed()) {
            try {
                subscriber.subscribe(listener, channelPublish(), channelReply());
            } catch (Exception ex) {
                database.getErrorService().capture(ex, "Error during listening for handshake " + this.getClass().getSimpleName());
                ex.printStackTrace();
            }
        }
    }

    public void stopListening() {
        if (listener != null) {
            if (listener.isSubscribed()) {
                listener.unsubscribe();
            }
        }
        if (subscriber != null) {
            subscriber.close();
            subscriber = null;
        }
    }

    public abstract Handshake create();

}
