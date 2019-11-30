/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.server.ServerService;
import dev.morphia.annotations.Entity;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
public class NetworkProfile extends NetworkPayload<UUID> {

    protected String identifier;
    protected String lastSeenServer;
    protected Date lastSeen = new Date();
    protected boolean online = false;
    protected transient UUID uuidID = null;

    @Inject
    public NetworkProfile(ServerService serverService) {
        super(serverService);
    }

    public boolean isOnlineOtherServer() {
        return isOnline() && (lastSeenServer == null || !lastSeenServer.equalsIgnoreCase(serverService.getThisServer().getName()));
    }

    public boolean isOnlineThisServer() {
        return isOnline() && lastSeenServer != null && lastSeenServer.equalsIgnoreCase(serverService.getThisServer().getName());
    }

    private void setUUID() {
        if (this.identifier != null && this.uuidID == null) {
            this.uuidID = UUID.fromString(identifier);
        }
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
            mostRecentServer = serverService.getThisServer().getName();
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

    @Override
    public UUID getIdentifier() {
        setUUID();
        return uuidID;
    }

    @Override
    public void setIdentifier(@Nonnull UUID identifier) {
        Preconditions.checkNotNull(identifier);
        this.identifier = identifier.toString();
        this.uuidID = identifier;
    }

}
