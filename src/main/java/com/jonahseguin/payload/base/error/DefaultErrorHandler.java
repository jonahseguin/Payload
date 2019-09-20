package com.jonahseguin.payload.base.error;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.PLang;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DefaultErrorHandler implements PayloadErrorHandler {

    @Override
    public void debug(PayloadCache cache, String message) {
        PayloadPlugin.get().getLogger().info("[Debug][" + cache.getName() + "] " + message);
        this.alertAdmins(cache, PLang.ADMIN_ALERT_CACHE_DEBUG, cache.getName(), message);
    }

    @Override
    public void error(PayloadCache cache, String message) {
        PayloadPlugin.get().getLogger().info("[Error][" + cache.getName() + "] " + message);
        this.alertAdmins(cache, PLang.ADMIN_ALERT_CACHE_ERROR, cache.getName(), message);
    }

    @Override
    public void exception(PayloadCache cache, Throwable throwable) {
        PayloadPlugin.get().getLogger().info("[Exception][" + cache.getName() + "] " + throwable.getMessage());
        if (this.isDebug()) {
            throwable.printStackTrace();
        }
        this.alertAdmins(cache, PLang.ADMIN_ALERT_CACHE_EXCEPTION, cache.getName(), throwable.getMessage());
    }

    @Override
    public void exception(PayloadCache cache, Throwable throwable, String message) {
        PayloadPlugin.get().getLogger().info("[Exception][" + cache.getName() + "] " + message + " - " + throwable.getMessage());
        if (this.isDebug()) {
            throwable.printStackTrace();
        }
        this.alertAdmins(cache, PLang.ADMIN_ALERT_CACHE_EXCEPTION, cache.getName(), message + " - " + throwable.getMessage());
    }

    private void alertAdmins(PayloadCache cache, PLang key, String... args) {
        final String finalMsg = ChatColor.translateAlternateColorCodes('&', cache.getLangController().get(key, args));
        PayloadPlugin.get().getServer().getOnlinePlayers().stream().filter(PayloadPermission.ADMIN::has).forEach(p -> ((Player) p).sendMessage(finalMsg));
    }

    public boolean isDebug() {
        return PayloadPlugin.get().isDebug();
    }

}
