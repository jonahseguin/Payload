/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.network;

import com.google.inject.Inject;
import com.jonahseguin.payload.server.PayloadServer;
import com.jonahseguin.payload.server.ServerService;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
public abstract class NetworkPayload<K> {

    protected transient final ServerService serverService;
    protected K identifier;
    protected ObjectId objectId;
    protected Date lastCached = new Date();
    protected Date lastSaved = new Date();
    protected boolean loaded = false;
    @Embedded
    protected Set<PayloadServer> loadedServers = new HashSet<>();

    @Inject
    public NetworkPayload(ServerService serverService) {
        this.serverService = serverService;
    }

    public NetworkPayload(ServerService serverService, K identifier, ObjectId objectId) {
        this.serverService = serverService;
        this.identifier = identifier;
        this.objectId = objectId;
    }

}
