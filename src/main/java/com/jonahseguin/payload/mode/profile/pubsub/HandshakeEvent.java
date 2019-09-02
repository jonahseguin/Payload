package com.jonahseguin.payload.mode.profile.pubsub;

public enum HandshakeEvent {

    REQUEST_PAYLOAD_SAVE, // player begins to join another server
    SAVING_PAYLOAD, // sent when the server is found
    SAVED_PAYLOAD // sent after the server they are leaving has saved their data to redis

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
