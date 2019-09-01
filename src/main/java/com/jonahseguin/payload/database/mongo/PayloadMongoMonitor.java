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
            this.database.databaseDebug("Attempting MongoDB initial connection");
        }
    }

    @Override
    public void serverHeartbeatSucceeded(ServerHeartbeatSucceededEvent event) {
        // Connected
        if (!this.database.getState().isMongoInitConnect()) {
            this.database.getHooks().forEach(DatabaseDependent::onMongoDbInitConnect);
            this.database.getState().setMongoInitConnect(true);
            this.database.databaseDebug("MongoDB initial connection succeeded");
        }
        else {
            if (!this.connected) {
                this.database.getHooks().forEach(DatabaseDependent::onMongoDbReconnect);
                this.database.databaseDebug("MongoDB re-connection succeeded");
            }
        }
        this.connected = true;
    }

    @Override
    public void serverHeartbeatFailed(ServerHeartbeatFailedEvent event) {
        // Lost connection or failed to connect
        if (this.connected) {
            this.database.getHooks().forEach(DatabaseDependent::onMongoDbDisconnect);
            this.database.databaseDebug("MongoDB disconnected");
        }
        this.connected = false;
    }
}
