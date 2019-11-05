package com.jonahseguin.payload.base.handshake;

import com.google.inject.Inject;
import com.jonahseguin.payload.database.DatabaseService;
import lombok.Getter;
import redis.clients.jedis.Jedis;

import javax.annotation.Nonnull;

@Getter
public abstract class Handshake {

    @Inject
    protected HandshakeService handshakeService;
    @Inject
    protected DatabaseService database;
    protected HandshakeListener listener;
    protected HandshakeHandler handler;

    @Inject
    public Handshake() {
        // No-args constructor required by Guice to create an instance
    }

    public abstract String channelPublish();

    public abstract String channelReply();

    public abstract void load(@Nonnull HandshakeData data);

    public abstract void write(@Nonnull HandshakeData data);

    /**
     * Called when receiving this handshake from another server, before the reply is sent.  Async.
     */
    public abstract void receive();

    public abstract boolean shouldAccept(@Nonnull HandshakeData data);

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
        listener = new HandshakeListener(handshakeService, this);
        try (Jedis subscriber = database.getJedisResource()) {
            subscriber.subscribe(listener, channelPublish(), channelReply());
        } catch (Exception ex) {
            database.getErrorService().capture(ex, "Error during listening for handshake " + this.getClass().getSimpleName());
        }
    }

    public void stopListening() {
        if (listener != null) {
            if (listener.isSubscribed()) {
                listener.unsubscribe();
            }
        }
    }

}
