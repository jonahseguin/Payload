/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.LangService;
import com.jonahseguin.payload.base.lang.PayloadLangService;
import com.jonahseguin.payload.base.listener.LockListener;
import com.jonahseguin.payload.command.PCommandHandler;
import com.jonahseguin.payload.mode.profile.listener.ProfileListener;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

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

    private final PayloadAPI api = new PayloadAPI(this);
    private static PayloadPlugin plugin = null;
    private Injector injector = null;
    private boolean locked = false;
    private final PayloadLocal local = new PayloadLocal(this);
    private PCommandHandler commandHandler;
    private LangService lang;

    /**
     * Format a string with arguments
     *
     * @param s    String
     * @param args Arguments
     * @return Formatted string, colorized
     */
    public static String format(String s, Object... args) {
        if (args != null) {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    if (s.contains("{" + i + "}")) {
                        s = s.replace("{" + i + "}", args[i] != null ? args[i].toString() : "null");
                    }
                }
            }
        }

        return ChatColor.translateAlternateColorCodes('&', s);
    }

    static PayloadPlugin getPlugin() {
        return plugin;
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

    @Override
    public void onEnable() {
        plugin = this;

        this.copyResources();
        if (!this.local.loadPayloadID()) {
            // Failed to load.  This will be handled by the method itself.
            this.getLogger().warning("[FATAL] Payload failed to load it's local file (payload.yml)");
        }
        if (this.local.isFirstStartup()) {
            this.getLogger().info("This is the first startup for Payload on this server instance.  Files created.");
        }

        injector = Guice.createInjector(Stage.PRODUCTION, PayloadAPI.install(this, "PayloadDatabase"));

        lang = new PayloadLangService(this);
        commandHandler = new PCommandHandler(this, lang, injector);

        this.getServer().getPluginManager().registerEvents(injector.getInstance(LockListener.class), this);
        this.getServer().getPluginManager().registerEvents(injector.getInstance(ProfileListener.class), this);
        this.getCommand("payload").setExecutor(this.commandHandler);
        this.getLogger().info(PayloadPlugin.format("Payload v{0} by Jonah Seguin enabled.", getDescription().getVersion()));
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

    /**
     * Is the Payload plugin globally in debug (used for default error handlers and similar)
     * @return True if debug is enabled
     */
    public boolean isDebug() {
        return this.local.isDebug();
    }

    @Override
    public void onDisable() {
        this.getLogger().info(PayloadPlugin.format("Payload v{0} by Jonah Seguin disabled.", getDescription().getVersion()));
        plugin = null;
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

    public void setDebug(boolean debug) {
        this.local.setDebug(debug);
        this.local.getConfig().set("debug", debug);
        try {
            this.local.getConfig().save(this.local.getPayloadFile());
        } catch (IOException ex) {
            getLogger().severe("Error saving config while setting debug status");
            if (this.isDebug()) {
                ex.printStackTrace();
            }
        }
    }

    public void alert(PayloadPermission permission, String module, String key, Object... args) {
        String l = lang.get(module, key, args);
        getLogger().info(l);
        getServer().getOnlinePlayers().stream().filter(p -> p.hasPermission(permission.getPermission())).forEach(p -> p.sendMessage(l));
    }

    public void alert(PayloadPermission permission, String msg) {
        getLogger().info(msg);
        getServer().getOnlinePlayers().stream().filter(p -> p.hasPermission(permission.getPermission())).forEach(p -> p.sendMessage(msg));
    }

}
