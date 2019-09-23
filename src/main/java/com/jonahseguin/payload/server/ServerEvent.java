package com.jonahseguin.payload.server;

public enum ServerEvent {

    JOIN("server-join"),
    PING("server-ping"),
    QUIT("server-quit"),
    UPDATE_NAME("server-update-name");

    private final String event;

    ServerEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }
}
