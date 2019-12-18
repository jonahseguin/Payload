/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.handshake;

import org.bson.Document;
import redis.clients.jedis.JedisPubSub;

public class HandshakeListener extends JedisPubSub {

    private final Handshake controller;
    private final HandshakeService service;

    public HandshakeListener(HandshakeService service, Handshake controller) {
        this.service = service;
        this.controller = controller;
    }

    @Override
    public void onMessage(String channel, String json) {
        if (channel.equalsIgnoreCase(controller.channelPublish())) {
            // Receiving init. handshake
            service.receive(controller.channelPublish(), mapData(json));
        } else if (channel.equalsIgnoreCase(controller.channelReply())) {
            // Receiving handshake reply
            service.receiveReply(controller.channelReply(), mapData(json));
        }
    }

    private HandshakeData mapData(String json) {
        Document document = Document.parse(json);
        return new HandshakeData(document);
    }

}
