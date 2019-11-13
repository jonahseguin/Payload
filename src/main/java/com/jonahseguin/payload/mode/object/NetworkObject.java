/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.inject.Inject;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.server.ServerService;
import dev.morphia.annotations.Entity;

import java.util.Date;

@Entity
public class NetworkObject extends NetworkPayload<String> {

    @Inject
    public NetworkObject(ServerService serverService) {
        super(serverService);
    }

    public void markLoaded() {
        loaded = true;
        loadedServers.add(serverService.getThisServer().getName());
        lastCached = new Date();
        mostRecentServer = serverService.getThisServer().getName();
    }

    public void markUnloaded() {
        loadedServers.remove(serverService.getThisServer().getName());
        loaded = loadedServers.size() > 0;
    }

    public void markSaved() {
        mostRecentServer = serverService.getThisServer().getName();
        lastSaved = new Date();
    }

}
