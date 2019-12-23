/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.network;

import com.google.inject.Inject;
import com.jonahseguin.payload.server.ServerService;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Transient;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;

@Getter
@Setter
@Entity
public abstract class NetworkPayload<K> {

    @Id
    private ObjectId id = new ObjectId(); // required for morphia mapping

    @Transient
    protected transient final ServerService serverService;
    protected long lastCached = System.currentTimeMillis();
    protected long lastSaved = System.currentTimeMillis();
    protected boolean loaded = false;
    @Embedded
    protected String mostRecentServer;

    @Inject
    public NetworkPayload(ServerService serverService) {
        this.serverService = serverService;
    }

    public boolean isThisMostRelevantServer() {
        if (mostRecentServer != null) {
            return mostRecentServer.equalsIgnoreCase(serverService.getThisServer().getName()) && loaded;
        }
        return false;
    }

    public abstract K getIdentifier();

    public abstract void setIdentifier(@Nonnull K identifier);

}
