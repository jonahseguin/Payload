/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database.mongo;

import com.jonahseguin.payload.database.PayloadDatabase;
import com.mongodb.event.ServerHeartbeatFailedEvent;
import com.mongodb.event.ServerHeartbeatStartedEvent;
import com.mongodb.event.ServerHeartbeatSucceededEvent;
import com.mongodb.event.ServerMonitorListener;

public class PayloadMongoMonitor implements ServerMonitorListener {

    private final PayloadDatabase database;

    public PayloadMongoMonitor(PayloadDatabase database) {
        this.database = database;
    }

    @Override
    public void serverHearbeatStarted(ServerHeartbeatStartedEvent event) {
        if (!this.database.getState().isMongoInitConnect()) {
            this.database.getErrorService().debug("Attempting MongoDB initial connection");
        }
    }

    @Override
    public void serverHeartbeatSucceeded(ServerHeartbeatSucceededEvent event) {
        if (!this.database.getState().isMongoInitConnect()) {
            this.database.getState().setMongoInitConnect(true);
            this.database.getErrorService().debug("MongoDB initial connection succeeded");
        }
        else {
            if (!database.getState().isMongoConnected()) {
                this.database.getErrorService().debug("MongoDB connection restored");
            }
        }
        this.database.getState().setMongoConnected(true);
    }

    @Override
    public void serverHeartbeatFailed(ServerHeartbeatFailedEvent event) {
        // Lost connection or failed to connect
        if (this.database.getState().isMongoConnected()) {
            this.database.getErrorService().capture("MongoDB connection lost");
        }
        this.database.getState().setMongoConnected(false);
    }
}
