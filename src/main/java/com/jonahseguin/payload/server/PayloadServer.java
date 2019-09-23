/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayloadServer {

    private String name;
    private long lastPing = 0;
    private boolean online = false;

    public PayloadServer(String name) {
        this.name = name;
    }

    public PayloadServer(String name, long lastPing, boolean online) {
        this.name = name;
        this.lastPing = lastPing;
        this.online = online;
    }
}
