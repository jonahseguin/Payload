/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import org.bson.Document;
import redis.clients.jedis.JedisPubSub;

public class ServerSubscriber extends JedisPubSub {

    private final ServerManager serverManager;

    public ServerSubscriber(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    @Override
    public void onMessage(String channel, String message) {
        ServerEvent event = ServerEvent.fromChannel(channel);
        if (event != null) {
            if (event.equals(ServerEvent.JOIN)) {
                if (!message.equalsIgnoreCase(this.serverManager.getThisServer().getName())) {
                    serverManager.handleJoin(message);
                }
            } else if (event.equals(ServerEvent.QUIT)) {
                if (!message.equalsIgnoreCase(this.serverManager.getThisServer().getName())) {
                    serverManager.handleQuit(message);
                }
            } else if (event.equals(ServerEvent.PING)) {
                if (!message.equalsIgnoreCase(this.serverManager.getThisServer().getName())) {
                    serverManager.handlePing(message);
                }
            } else if (event.equals(ServerEvent.UPDATE_NAME)) {
                Document data = Document.parse(message);
                String oldName = data.getString("old");
                String newName = data.getString("new");
                serverManager.handleUpdateName(oldName, newName);
            }
        }
    }
}
