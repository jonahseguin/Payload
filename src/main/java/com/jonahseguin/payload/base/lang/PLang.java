package com.jonahseguin.payload.base.lang;

/**
 * This enumeration is all of the messages used within Payload that will be shown to end users (players) in-game or otherwise.
 * The messages are implemented and can be customized via the {@link PayloadLangController} class.
 * Each PayloadCache has it's own language controller and thus language definitions are cache-specific.
 * Here, the keys for messages as well as the default values can be shown.
 *
 *
 *
 */
public enum PLang {

    FAILED_TO_LOAD_PAYLOAD_FILE("&7[Payload] &4[Fatal] Couldn't load payload.yml file.  Aborting startup and locking server."),
    KICK_MESSAGE_LOCKED("&4The server is currently locked for maintenance.  We are working on resolving this problem as soon as possible.  Please try again soon."),
    KICK_MESSAGE_ADMIN_LOCKED("&7[Payload] &4[Fatal] &cThe server has been locked to players due to a fatal error during Payload startup.  Try restarting the server, and check the error logs for details."),
    CACHE_LOCKED("&7[Payload] The cache &b{0} &7has been &clocked&7. ({1})"), // argument is for the reason
    CACHE_UNLOCKED("&7[Payload] The cache &b{0} &7has been &aunlocked&7. ({1})"),  // argument is for the reason
    ;

    private final String text;

    PLang(String text) {
        this.text = text;
    }

    public String get() {
        return this.text;
    }

}
