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

    ERROR_FAILED_TO_LOAD_PAYLOAD_FILE("&7[Payload] &4[Fatal] Couldn't load payload.yml file.  Aborting startup and locking server."),
    KICK_MESSAGE_LOCKED("&4The server is currently locked for maintenance.  We are working on resolving this problem as soon as possible.  Please try again soon."),
    KICK_MESSAGE_ADMIN_LOCKED("&7[Payload] &4[Fatal] &cThe server has been locked to players due to a fatal error during Payload startup.  Try restarting the server, and check the error logs for details."),
    CACHE_LOCKED("&7[Payload] The cache &b{0} &7has been &clocked&7. ({1})"), // argument is for the reason
    CACHE_UNLOCKED("&7[Payload] The cache &b{0} &7has been &aunlocked&7. ({1})"),  // argument is for the reason
    UNKNOWN_COMMAND("&cPayload: Sub-command '{0}' does not exist.  Type /payload help for a list of commands."),
    COMMAND_NO_PERMISSION("&cPayload: You do not have permission to use this command."),
    COMMAND_PLAYER_ONLY("&c:Payload: This is a player-only command."),
    ADMIN_ALERT_CACHE_DEBUG("&7[Payload] &a[Debug] &7{0}: &f{1}"),
    ADMIN_ALERT_CACHE_ERROR("&7[Payload] &c[Error] &7{0}: &f{1}"),
    ADMIN_ALERT_CACHE_EXCEPTION("&7[Payload] &4[Exception] &7{0}: &f{1}"),
    CACHE_FAILURE_PROFILE_ATTEMPT_SUCCESS("&7[&6{0}&7] &aYour profile was loaded successfully."),
    CACHE_FAILURE_PROFILE_ATTEMPT_FAILURE("&7[&6{0}&7] &cProfile loading attempt failed.  Trying again in {0} seconds..."),
    CACHE_FAILURE_PROFILE_ATTEMPT("&7[&6{0}&7] Attempting to load your profile..."),
    CACHE_FAILURE_PROFILE_NOTICE("&7[&6{0}&7] &4Your profile failed to load.  We will automatically attempt to re-load it every {0} seconds."),
    PLAYER_ONLINE_NO_PROFILE("&cYou appear to have no profile loaded.  Attempting to fix..."),
    DEBUG_DATABASE("&7[Payload][Debug][Database: {0}] {1}"),
    ERROR_DATABASE("&7[Payload]&7[&cError&7][Database: {0}] {1} ({2})")
    ;

    private final String text;

    PLang(String text) {
        this.text = text;
    }

    public String get() {
        return this.text;
    }

}
