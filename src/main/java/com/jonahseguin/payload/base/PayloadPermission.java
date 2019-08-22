package com.jonahseguin.payload.base;

import org.bukkit.permissions.Permissible;

public enum PayloadPermission {

    DEBUG("debug"),
    ADMIN("admin"),
    NONE("")

    ;

    private final String permission;

    PayloadPermission(String permission) {
        this.permission = "payload." + permission;
    }

    public String getPermission() {
        return permission;
    }

    public boolean has(Permissible permissible) {
        if (this.permission.length() == 0) {
            return true; // if its empty, assume this is the NONE permission which everyone has by default
        }
        return permissible.hasPermission(this.getPermission());
    }

    public static boolean has(Permissible permissible, PayloadPermission permission) {
        return permission.has(permissible);
    }

}
