/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import org.bson.Document;

public class ServerPublisher {

    private final PayloadServerService payloadServerService;

    public ServerPublisher(PayloadServerService serverService) {
        this.payloadServerService = serverService;
    }

    public void publishPing() {
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                payloadServerService.getDatabase().getRedisPubSub().async().publish(ServerEvent.PING.getEvent(), payloadServerService.getThisServer().getName());
            } catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing PING event");
            }
        });
    }

    public void publishJoin() {
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                payloadServerService.getDatabase().getRedisPubSub().async().publish(ServerEvent.JOIN.getEvent(), payloadServerService.getThisServer().getName());
            } catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing JOIN event");
            }
        });
    }

    public void publishQuit() {
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                payloadServerService.getDatabase().getRedisPubSub().async().publish(ServerEvent.QUIT.getEvent(), payloadServerService.getThisServer().getName());
            } catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing QUIT event");
            }
        });
    }

    public void publishUpdateName(String oldName, String newName) {
        this.payloadServerService.getPayloadPlugin().getServer().getScheduler().runTaskAsynchronously(payloadServerService.getPayloadPlugin(), () -> {
            try {
                Document data = new Document();
                data.append("old", oldName);
                data.append("new", newName);
                payloadServerService.getDatabase().getRedisPubSub().async().publish(ServerEvent.UPDATE_NAME.getEvent(), data.toJson());
            } catch (Exception ex) {
                payloadServerService.getDatabase().getErrorService().capture(ex, "Payload Server Service: Error publishing UPDATE_NAME event");
            }
        });
    }

}
