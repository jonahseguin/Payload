package com.jonahseguin.payload.server;

import redis.clients.jedis.JedisPubSub;

public class ServerSubscriber extends JedisPubSub {

    private final ServerManager serverManager;

    public ServerSubscriber(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public void onMessage(String channel, String message) {
        // todo
    }
}
