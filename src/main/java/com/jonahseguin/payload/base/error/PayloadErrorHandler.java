package com.jonahseguin.payload.base.error;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.type.Payload;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public interface PayloadErrorHandler<K, X extends Payload> {

    default void debug(String message) {
        Bukkit.getLogger().info(PayloadPlugin.PREFIX + message);
        for (Player pl : PayloadPlugin.get().getServer().getOnlinePlayers()) {
            if (PayloadPermission.DEBUG.has(pl)) {
                pl.sendMessage(ChatColor.GRAY + "[Payload][Debug] " + message);
            }
        }
    }

    void exception(Throwable throwable);

    void error(String message);

    void error(PayloadCache<K, X> cache, String message);

    void exception(PayloadCache<K, X> cache, Throwable throwable);

    void exception(Throwable throwable, String message);

    void exception(PayloadCache<K, X> cache, Throwable throwable, String message);

}
