/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.lang;

public enum PLang {

    NO_PERMISSION("&cNo permission."),
    PLAYER_ONLY("&cThis command is player-only."),
    INCORRECT_USAGE("&cIncorrect usage. (needs {0} arguments)  Use: {1} {2}"),
    UNKNOWN_COMMAND("&cUnknown command: '{0}'.  Use /payload for help."),

    JOIN_DENY_DATABASE("&c[{0}] The database is currently offline.  Please try again soon, we are working to resolve this issue as soon as possible."),

    ;

    private final String lang;

    PLang(String lang) {
        this.lang = lang;
    }

    public String getLang() {
        return lang;
    }

    public PLang[] getAll() {
        return PLang.values();
    }

    public PLang fromString(String s) {
        return PLang.valueOf(s.toUpperCase());
    }

}
