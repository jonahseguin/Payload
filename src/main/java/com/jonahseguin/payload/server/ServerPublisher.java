/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import org.bson.Document;
import redis.clients.jedis.Jedis;

public class ServerPublisher {

    private final PayloadServerService payloadServerService;

    public ServerPublisher(PayloadServerService serverService) {
        this.payloadServerService = serverService;
    }

    public void publishPing() {
        this.payloadServerService.getExecutorService().submit(() -> {
            try (Jedis jedis = this.payloadServerService.getDatabase().getJedisResource()) {
                jedis.publish(ServerEvent.PING.getEvent(), payloadServerService.getThisServer().getName());
            }
            catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Server Manager: Error publishing PING event");
            }
        });
    }

    public void publishJoin() {
        this.payloadServerService.getExecutorService().submit(() -> {
            try (Jedis jedis = this.payloadServerService.getDatabase().getJedisResource()) {
                jedis.publish(ServerEvent.JOIN.getEvent(), payloadServerService.getThisServer().getName());
            }
            catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Server Manager: Error publishing JOIN event");
            }
        });
    }

    public void publishQuit() {
        // Sync -- we want this to complete first before shutdown
        this.payloadServerService.getExecutorService().submit(() -> {
            try (Jedis jedis = this.payloadServerService.getDatabase().getJedisResource()) {
                jedis.publish(ServerEvent.QUIT.getEvent(), payloadServerService.getThisServer().getName());
            }
            catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Server Manager: Error publishing QUIT event");
            }
        });
    }

    public void publishUpdateName(String oldName, String newName) {
        this.payloadServerService.getExecutorService().submit(() -> {
            try (Jedis jedis = this.payloadServerService.getDatabase().getJedisResource()) {
                Document data = new Document();
                data.append("old", oldName);
                data.append("new", newName);
                jedis.publish(ServerEvent.UPDATE_NAME.getEvent(), data.toJson());
            }
            catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Server Manager: Error publishing UPDATE_NAME event");
            }
        });
    }

}
