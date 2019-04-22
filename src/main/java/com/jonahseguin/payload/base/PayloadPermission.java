package com.jonahseguin.payload.base;

import org.bukkit.permissions.Permissible;

public enum PayloadPermission {

    DEBUG("debug"),
    ADMIN("admin")

    ;

    private final String permission;

    PayloadPermission(String permission) {
        this.permission = "payload." + permission;
    }

    public String getPermission() {
        return permission;
    }

    public boolean has(Permissible permissible) {
        return permissible.hasPermission(this.getPermission());
    }

    public static boolean has(Permissible permissible, PayloadPermission permission) {
        return permission.has(permissible);
    }

}
