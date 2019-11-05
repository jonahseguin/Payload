package com.jonahseguin.payload.mode.profile;

import com.google.inject.Inject;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.server.PayloadServer;
import com.jonahseguin.payload.server.ServerService;
import dev.morphia.annotations.Entity;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
public class NetworkProfile extends NetworkPayload<UUID> {

    protected PayloadServer lastSeenServer;
    protected boolean online = false;
    protected Date lastSeen = new Date();

    @Inject
    public NetworkProfile(ServerService serverService) {
        super(serverService);
    }

    public boolean isOnlineThisServer() {
        return online && lastSeenServer != null && lastSeenServer.getName().equalsIgnoreCase(serverService.getThisServer().getName());
    }

    public void markLoaded(boolean online) {
        this.online = online;
        loaded = true;
        loadedServers.add(serverService.getThisServer());
        lastCached = new Date();
        if (online) {
            lastSeenServer = serverService.getThisServer();
            lastSeen = new Date();
        }
    }

    public void markUnloaded(boolean switchingServers) {
        online = switchingServers;
        loadedServers.remove(serverService.getThisServer());
        loaded = loadedServers.size() > 0 || switchingServers;
        lastSeen = new Date();
    }

    public void markSaved() {
        lastSaved = new Date();
        if (isOnlineThisServer()) {
            lastSeen = new Date();
        }
    }

}
