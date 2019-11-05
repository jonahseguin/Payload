package com.jonahseguin.payload.database.mongo;

import com.jonahseguin.payload.database.DatabaseDependent;
import com.jonahseguin.payload.database.DatabaseService;
import com.mongodb.event.ServerHeartbeatFailedEvent;
import com.mongodb.event.ServerHeartbeatStartedEvent;
import com.mongodb.event.ServerHeartbeatSucceededEvent;
import com.mongodb.event.ServerMonitorListener;

public class PayloadMongoMonitor implements ServerMonitorListener {

    private final DatabaseService database;
    private boolean connected = false;

    public PayloadMongoMonitor(DatabaseService database) {
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
