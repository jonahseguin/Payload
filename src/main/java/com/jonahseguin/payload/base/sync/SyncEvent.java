/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

public enum SyncEvent {

    UPDATE("update"), // Update from DB
    UNCACHE("uncache"), // Remove from local cache

    REQUEST_SAVE("request-save"), // Request that a payload be saved [if shouldSave()]
    REQUEST_SAVE_ALL("request-save-all"), // Request that all payloads be saved [if shouldSave()]

    SAVE_COMPLETED("save-completed"),
    SAVE_ALL_COMPLETED("save-all-completed")
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
