package com.jonahseguin.payload;

import java.net.InetAddress;

import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.lang.PayloadLang;
import com.jonahseguin.payload.base.lang.PayloadLangController;
import com.jonahseguin.payload.base.listener.LockListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 6:44 PM
 *
 * This class is just here so that Payload can be used as a dependency via
 * loaded plugin instead of being shaded.
 *
 * Also provides some utility methods.
 */
public class PayloadPlugin extends JavaPlugin {

    public static final String PREFIX = "[Payload] ";

    private static PayloadPlugin instance = null;

    private boolean locked = false;
    private final PayloadLangController globalLangController = new PayloadLangController();

    public static String format(String s, String... args) {
        if (args != null) {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    if (s.contains("{" + i + "}")) {
                        s = s.replace("{" + i + "}", args[i]);
                    }
                }
            }
        }

        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String getIP(InetAddress inetAddress) {
        return inetAddress.toString().split("/")[1];
    }

    public static void runASync(Plugin plugin, Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static PayloadPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        PayloadPlugin.instance = this;
        getServer().getPluginManager().registerEvents(new LockListener(), this);
        getLogger().info(PayloadPlugin.format("Payload v{0} by Jonah Seguin enabled.", PayloadPlugin.get().getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        getLogger().info(PayloadPlugin.format("Payload v{0} by Jonah Seguin disabled.", PayloadPlugin.get().getDescription().getVersion()));
        PayloadPlugin.instance = null;
    }

    /**
     * Whether to lock the server from players joining that don't have the bypass permission
     * @return Locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Change the status of server join lock
     * @param locked is it locked?
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void alert(PayloadPermission required, PLang lang, String... args) {
        Bukkit.getLogger().info(this.globalLangController.get(lang, args));
        for (Player pl : getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(this.globalLangController.get(lang, args));
            }
        }
    }

    public PayloadLangController getGlobalLangController() {
        return globalLangController;
    }
}
