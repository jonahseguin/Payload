/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database.mongo;

import com.jonahseguin.payload.database.DatabaseDependent;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.mongodb.event.ServerHeartbeatFailedEvent;
import com.mongodb.event.ServerHeartbeatStartedEvent;
import com.mongodb.event.ServerHeartbeatSucceededEvent;
import com.mongodb.event.ServerMonitorListener;

public class PayloadMongoMonitor implements ServerMonitorListener {

    private final PayloadDatabase database;
    private boolean connected = false;

    public PayloadMongoMonitor(PayloadDatabase database) {
        this.database = database;
    }

    @Override
    public void serverHearbeatStarted(ServerHeartbeatStartedEvent event) {
        if (!this.connected && !this.database.getState().isMongoInitConnect()) {
            // MongoDB connection attempting for first time
            this.database.getErrorService().debug("Attempting MongoDB initial connection");
        }
    }

    @Override
    public void serverHeartbeatSucceeded(ServerHeartbeatSucceededEvent event) {
        // Connected
        this.database.getState().setMongoConnected(true);
        if (!this.database.getState().isMongoInitConnect()) {
            this.database.getHooks().forEach(DatabaseDependent::onMongoDbInitConnect);
            this.database.getState().setMongoInitConnect(true);
            this.database.getErrorService().debug("MongoDB initial connection succeeded");
        }
        else {
            if (!this.connected) {
                this.database.getHooks().forEach(DatabaseDependent::onMongoDbReconnect);
                this.database.getErrorService().debug("MongoDB re-connection succeeded");
            }
        }
        this.connected = true;
    }

    @Override
    public void serverHeartbeatFailed(ServerHeartbeatFailedEvent event) {
        // Lost connection or failed to connect
        this.database.getState().setMongoConnected(false);
        if (this.connected) {
            this.database.getHooks().forEach(DatabaseDependent::onMongoDbDisconnect);
            this.database.getErrorService().debug("MongoDB disconnected");
        }
        this.connected = false;
    }
}
