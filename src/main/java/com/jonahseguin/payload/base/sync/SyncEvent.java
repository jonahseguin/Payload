/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

public enum SyncEvent {

    UPDATE("update"), // Update from DB
    UNCACHE("uncache") // Remove from local cache

    ;

    private final String name;

    SyncEvent(String name) {
        this.name = name;
    }

    public static SyncEvent fromString(String s) {
        for (SyncEvent e : values()) {
            if (e.getName().equalsIgnoreCase(s)) {
                return e;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

}
