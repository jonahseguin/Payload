package com.jonahseguin.payload.base.database;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import com.mongodb.event.ServerHeartbeatFailedEvent;
import com.mongodb.event.ServerHeartbeatStartedEvent;
import com.mongodb.event.ServerHeartbeatSucceededEvent;
import com.mongodb.event.ServerMonitorListener;

public class PayloadMongoMonitor<K, X extends Payload> implements ServerMonitorListener {

    private final PayloadCache<K, X> cache;

    public PayloadMongoMonitor(PayloadCache<K, X> cache) {
        this.cache = cache;
    }

    @Override
    public void serverHearbeatStarted(ServerHeartbeatStartedEvent event) {
        cache.getErrorHandler().debug("MongoDB connection heartbeat starting, waiting for connection");

    }

    @Override
    public void serverHeartbeatSucceeded(ServerHeartbeatSucceededEvent event) {

    }

    @Override
    public void serverHeartbeatFailed(ServerHeartbeatFailedEvent event) {

    }
}
