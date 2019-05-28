package com.jonahseguin.payload.base.error;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.type.Payload;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public interface PayloadErrorHandler {

    default void debug(String cacheName, String message) {
        Bukkit.getLogger().info(PayloadPlugin.PREFIX + message);
        for (Player pl : PayloadPlugin.get().getServer().getOnlinePlayers()) {
            if (PayloadPermission.DEBUG.has(pl)) {
                pl.sendMessage(ChatColor.GRAY + "[Payload][Debug][" + cacheName + "] " + message);
            }
        }
    }

    void error(String cacheName, String message);

    void exception(String cacheName, Throwable throwable);

    void exception(String cacheName, Throwable throwable, String message);

    boolean isDebug();

}
