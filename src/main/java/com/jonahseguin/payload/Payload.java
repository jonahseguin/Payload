package com.jonahseguin.payload;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.type.CacheSettings;
import com.jonahseguin.payload.util.CacheBuilder;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 6:44 PM
 */
public class Payload {

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

    public static void runASync(Plugin plugin, Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static <T extends Profile> ProfileCache<T> create(CacheSettings<T> settings, Class<T> clazz) {
        return new ProfileCache<>(settings, clazz);
    }

}
