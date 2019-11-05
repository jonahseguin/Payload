package com.jonahseguin.payload.mode.object;

import com.google.inject.Inject;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.server.ServerService;

import java.util.Date;

public class NetworkObject extends NetworkPayload<String> {

    @Inject
    public NetworkObject(ServerService serverService) {
        super(serverService);
    }

    public void markLoaded() {
        loaded = true;
        loadedServers.add(serverService.getThisServer());
        lastCached = new Date();
    }

    public void markUnloaded() {
        loadedServers.remove(serverService.getThisServer());
        loaded = loadedServers.size() > 0;
    }

    public void markSaved() {
        lastSaved = new Date();
    }

}
