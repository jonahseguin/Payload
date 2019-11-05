/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.inject.Inject;
import com.jonahseguin.payload.base.network.NetworkPayload;
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

    protected String lastSeenServer;
    protected Date lastSeen = new Date();
    protected boolean online = false;

    @Inject
    public NetworkProfile(ServerService serverService) {
        super(serverService);
    }

    public boolean isOnlineThisServer() {
        return isOnline() && lastSeenServer != null && lastSeenServer.equalsIgnoreCase(serverService.getThisServer().getName());
    }

    public boolean isOnline() {
        boolean shouldBeOnline = (System.currentTimeMillis() - lastSeen.getTime()) < (1000 * 60 * 60);
        if (online && !shouldBeOnline) {
            online = false;
        }
        return online;
    }

    public void markLoaded(boolean online) {
        this.online = online;
        loaded = true;
        loadedServers.add(serverService.getThisServer().getName());
        lastCached = new Date();
        if (online) {
            lastSeenServer = serverService.getThisServer().getName();
            lastSeen = new Date();
        }
    }

    public void markUnloaded(boolean switchingServers) {
        online = switchingServers;
        loadedServers.remove(serverService.getThisServer().getName());
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
