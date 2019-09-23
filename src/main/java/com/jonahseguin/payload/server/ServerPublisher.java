/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import com.jonahseguin.payload.PayloadPlugin;
import org.bson.Document;
import redis.clients.jedis.Jedis;

public class ServerPublisher {

    private final ServerManager serverManager;

    public ServerPublisher(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public void publishPing() {
        PayloadPlugin.runASync(PayloadPlugin.get(), () -> {
            try (Jedis jedis = this.serverManager.getDatabase().getResource()) {
                jedis.publish(ServerEvent.PING.getEvent(), serverManager.getThisServer().getName());
            }
        });
    }

    public void publishJoin() {
        PayloadPlugin.runASync(PayloadPlugin.get(), () -> {
            try (Jedis jedis = this.serverManager.getDatabase().getResource()) {
                jedis.publish(ServerEvent.JOIN.getEvent(), serverManager.getThisServer().getName());
            }
        });
    }

    public void publishQuit() {
        PayloadPlugin.runASync(PayloadPlugin.get(), () -> {
            try (Jedis jedis = this.serverManager.getDatabase().getResource()) {
                jedis.publish(ServerEvent.QUIT.getEvent(), serverManager.getThisServer().getName());
            }
        });
    }

    public void publishUpdateName(String oldName, String newName) {
        PayloadPlugin.runASync(PayloadPlugin.get(), () -> {
            try (Jedis jedis = this.serverManager.getDatabase().getResource()) {
                Document data = new Document();
                data.append("old", oldName);
                data.append("new", newName);
                jedis.publish(ServerEvent.QUIT.getEvent(), data.toJson());
            }
        });
    }

}
