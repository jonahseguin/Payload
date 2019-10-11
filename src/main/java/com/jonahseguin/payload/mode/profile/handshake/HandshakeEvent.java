/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.handshake;

public enum HandshakeEvent {

    REQUEST_PAYLOAD_SAVE, // player begins to join another server
    SAVING_PAYLOAD, // sent when the server is found
    SAVED_PAYLOAD, // sent after the server they are leaving has saved their data to redis

    PAYLOAD_NOT_CACHED_CONTINUE // sent if a server is requested to save, but the uuid that the save was requested for isn't cached/online
    // in this case, the server requesting the save (the server the player is connecting to)
    // will just use the payload they already loaded when getting the lastSeenServer field
    ;

    private final String name;

    HandshakeEvent() {
        this.name = toString().toLowerCase();
    }

    HandshakeEvent(String name) {
        this.name = name;
    }

    public static HandshakeEvent fromString(String s) {
        for (HandshakeEvent e : HandshakeEvent.values()) {
            if (e.name.equalsIgnoreCase(s)) {
                return e;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

}
