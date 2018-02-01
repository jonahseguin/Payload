package com.jonahseguin.payload;

import java.net.InetAddress;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 6:44 PM
 */
public class Payload extends JavaPlugin {

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

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

}
