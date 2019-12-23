/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.network;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.server.ServerService;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Transient;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;
import java.util.UUID;

@Entity("NetworkProfile")
@Getter
@Setter
public class NetworkProfile {

    @Id
    private ObjectId id = new ObjectId(); // required for morphia mapping

    @Transient
    protected transient final ServerService serverService;
    protected long lastCached = System.currentTimeMillis();
    protected long lastSaved = System.currentTimeMillis();

    protected String identifier;
    protected String lastSeenServer;
    protected long lastSeen = 0L;
    protected boolean online = false;
    protected transient UUID uuidID = null;

    @Inject
    public NetworkProfile(ServerService serverService) {
        this.serverService = serverService;
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
        boolean shouldBeOnline = (System.currentTimeMillis() - lastSeen) < (1000 * 60 * 60);
        if (online && !shouldBeOnline) {
            online = false;
        }
        return online;
    }

    public void markLoaded(boolean online) {
        this.online = online;
        lastCached = System.currentTimeMillis();
        if (online) {
            lastSeenServer = serverService.getThisServer().getName();
            lastSeen = System.currentTimeMillis();
        }
    }

    public void markUnloaded(boolean switchingServers) {
        online = switchingServers;
        lastSeen = System.currentTimeMillis();
    }

    public void markSaved() {
        lastSaved = System.currentTimeMillis();
        if (isOnlineThisServer()) {
            lastSeen = System.currentTimeMillis();
        }
    }

    public UUID getIdentifier() {
        setUUID();
        return uuidID;
    }

    public void setIdentifier(@Nonnull UUID identifier) {
        Preconditions.checkNotNull(identifier);
        this.identifier = identifier.toString();
        this.uuidID = identifier;
    }

}
