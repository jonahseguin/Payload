/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.common.collect.HashBiMap;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.data.PayloadLocal;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.lang.PayloadLangController;
import com.jonahseguin.payload.base.listener.LockListener;
import com.jonahseguin.payload.command.PCommandHandler;
import com.jonahseguin.payload.mode.profile.listener.ProfileListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 6:44 PM
 *
 * Main Bukkit/Spigot JavaPlugin class, entrance point of this piece of software
 */
@Singleton
@Getter
public class PayloadPlugin extends JavaPlugin {

    public static final String PREFIX = "[Payload] ";

    private static PayloadPlugin plugin;
    private Injector injector = null;

    private boolean locked = true;
    private final PayloadLangController globalLangController = new PayloadLangController();
    @Inject private PayloadLocal local;
    @Inject private PCommandHandler commandHandler;
    private final HashBiMap<String, UUID> uuids = HashBiMap.create(); // <Username, UUID>

    @Override
    public void onEnable() {
        PayloadModule module = new PayloadModule(this, this);
        this.injector = Guice.createInjector(module);
        this.injector.injectMembers(this);

        this.copyResources();
        if (!this.local.loadPayloadID()) {
            // Failed to load.  This will be handled by the method itself.
            this.getLogger().warning("[FATAL] Payload failed to load it's local file (payload.yml)");
        }
        if (this.local.isFirstStartup()) {
            this.getLogger().info("This is the first startup for Payload on this server instance.  Files created.");
        }
        this.getServer().getPluginManager().registerEvents(new LockListener(), this);
        this.getServer().getPluginManager().registerEvents(new ProfileListener(), this);
        this.getCommand("payload").setExecutor(this.commandHandler);
        this.getLogger().info(PayloadPlugin.format("Payload v{0} by Jonah Seguin enabled.", getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        this.getLogger().info(PayloadPlugin.format("Payload v{0} by Jonah Seguin disabled.", getDescription().getVersion()));
    }

    private void copyResources() {
        if (!new File(getDataFolder(), "payload.yml").exists()) {
            this.saveResource("payload.yml", false);
            getLogger().info("Generated default payload.yml");
        }
        if (!new File(getDataFolder(), "database.yml").exists()) {
            this.saveResource("database.yml", false); // Copy the database.yml file from jar to plugin folder, don't replace if exists
            getLogger().info("Generated default database.yml");
        }
    }

    public HashBiMap<String, UUID> getUUIDs() {
        return uuids;
    }

    public void saveUUID(String username, UUID uuid) {
        this.uuids.put(username.toLowerCase(), uuid);
    }

    public UUID getUUID(String username) {
        return this.uuids.get(username.toLowerCase());
    }

    public String getUsername(UUID uuid) {
        return this.uuids.inverse().get(uuid);
    }

    /**
     * Format a string with arguments
     * @param s String
     * @param args Arguments
     * @return Formatted string, colorized
     */
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

    /**
     * Get the simple IP address from an {@link InetAddress}
     * @param inetAddress The InetAddress
     * @return The simple IP in {@link String} form
     */
    public static String getIP(InetAddress inetAddress) {
        return inetAddress.toString().split("/")[1];
    }

    /**
     * Run a task async. via Bukkit scheduler
     * @param plugin {@link JavaPlugin} to run via
     * @param runnable What to run
     */
    public static void runASync(Plugin plugin, Runnable runnable) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    /**
     * Whether to globally lock the server from players joining that don't have the bypass permission
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

    /**
     * Get the Local ID handler for this Payload instance on a specific server instance
     * The {@link PayloadLocal} instance can be used to get the unique ID for this server.
     * @return PayloadLocal
     */
    public PayloadLocal getLocal() {
        return this.local;
    }

    public void alert(PayloadPermission required, PLang lang, String... args) {
        Bukkit.getLogger().info(this.globalLangController.get(lang, args));
        for (Player pl : getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(this.globalLangController.get(lang, args));
            }
        }
    }

    public void alert(PayloadPermission required, String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        Bukkit.getLogger().info(msg);
        for (Player pl : getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(msg);
            }
        }
    }

    /**
     * Is the Payload plugin globally in debug (used for default error handlers and similar)
     * @return True if debug is enabled
     */
    public boolean isDebug() {
        return this.local.isDebug();
    }

    public void setDebug(boolean debug) {
        this.local.setDebug(true);
        this.local.getConfig().set("debug", true);
        try {
            this.local.getConfig().save(this.local.getPayloadFile());
        } catch (IOException ex) {
            alert(PayloadPermission.DEBUG, "&cError setting debug mode to " + debug + ": while saving to payload.yml: " + ex.getMessage());
            if (this.isDebug()) {
                ex.printStackTrace();
            }
        }
    }


    /**
     * Get the Global plugin-wide default Language Controller
     * @return {@link PayloadLangController} default
     */
    public PayloadLangController getGlobalLangController() {
        return globalLangController;
    }

    /**
     * Get the Payload Command Handler
     * For internal Payload use only.
     *
     * @return PCommandHandler
     */
    public PCommandHandler getCommandHandler() {
        return commandHandler;
    }

    public ClassLoader getPayloadClassLoader() {
        return this.getClassLoader();
    }

    private static PayloadPlugin getPlugin() {
        return plugin;
    }

    public static PayloadModule install(Plugin plugin) {
        return new PayloadModule(PayloadPlugin.getPlugin(), plugin);
    }

}
