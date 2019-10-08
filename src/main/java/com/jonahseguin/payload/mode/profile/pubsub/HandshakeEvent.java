/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.pubsub;

public enum HandshakeEvent {

    REQUEST_PAYLOAD_SAVE, // player begins to join another server
    SAVING_PAYLOAD, // sent when the server is found
    SAVED_PAYLOAD, // sent after the server they are leaving has saved their data to redis

    PAYLOAD_NOT_CACHED_CONTINUE // sent if a server is requested to save, but the uuid that the save was requested for isn't cached/online
    // in this case, the server requesting the save (the server the player is connecting to)
    // will just use the payload they already loaded when getting the lastSeenServer field
    ;

    /*
    -> cache.save(payload)
    - check if player is online another server
    - if they are online another server, publish Handshake Event: PAYLOAD_SAVED_ELSEWHERE

-> other server receives PAYLOAD_SAVED_ELSEWHERE
    - re-cache their profile from the database (Redis)


this will ensure no data loss/rollback/overwrites.

// -----------------------------------------------------------------------------------------

-> cache.prepareUpdate(payload, callback(newPayload))
  - if profile online another server: publish PAYLOAD_PREPARE_UPDATE
  - if they're not, just save after the callback
-> other server receives PAYLOAD_PREPARE_UPDATE
  - if they have the profile online, (or even if they don't):
  - if they are online, save them
  - then publish PAYLOAD_UPDATE_PREPARED
-> then the source server calls it's callback provided in #prepareUpdate, and then makes a cache.save(payload) call after saving it

cache.prepareUpdate(payload, (updatedPayload) -> {
  updatedPayload.setPvPTimer(60);
  });

     */

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
