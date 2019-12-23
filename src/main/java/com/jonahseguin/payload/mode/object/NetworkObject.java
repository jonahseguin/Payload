/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.server.ServerService;
import dev.morphia.annotations.Entity;

import javax.annotation.Nonnull;

@Entity
public class NetworkObject extends NetworkPayload<String> {

    private String identifier = "";

    @Inject
    public NetworkObject(ServerService serverService) {
        super(serverService);
    }

    public void markLoaded() {
        loaded = true;
        lastCached = System.currentTimeMillis();
        mostRecentServer = serverService.getThisServer().getName();
    }

    public void markUnloaded() {
        loaded = false;
    }

    public void markSaved() {
        mostRecentServer = serverService.getThisServer().getName();
        lastSaved = System.currentTimeMillis();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(@Nonnull String identifier) {
        Preconditions.checkNotNull(identifier);
        this.identifier = identifier;
    }
}
