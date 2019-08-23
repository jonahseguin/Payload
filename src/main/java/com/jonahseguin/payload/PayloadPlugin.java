package com.jonahseguin.payload;

import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.data.PayloadLocal;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.lang.PayloadLangController;
import com.jonahseguin.payload.base.listener.LockListener;
import com.jonahseguin.payload.command.PCommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.net.InetAddress;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 6:44 PM
 *
 * Main Bukkit/Spigot JavaPlugin class, entrance point of this piece of software
 */
public class PayloadPlugin extends JavaPlugin {

    public static final String PREFIX = "[Payload] ";

    private static PayloadPlugin instance = null;

    private boolean locked = true;
    private boolean debug = false;
    private final PayloadLangController globalLangController = new PayloadLangController();
    private final PayloadLocal local = new PayloadLocal();
    private final PCommandHandler commandHandler = new PCommandHandler();

    public PayloadPlugin() {
        if (PayloadPlugin.instance != null) {
            throw new IllegalStateException("PayloadPlugin has already been created");
        }
    }

    public PayloadPlugin(PluginLoader loader, Server server, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, server, description, dataFolder, file);
        if (PayloadPlugin.instance != null) {
            throw new IllegalStateException("PayloadPlugin has already been created");
        }
    }

    public PayloadPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        if (PayloadPlugin.instance != null) {
            throw new IllegalStateException("PayloadPlugin has already been created");
        }
    }

    @Override
    public void onEnable() {
        PayloadPlugin.instance = this;
        this.copyResources();
        if (!this.local.loadPayloadID()) {
            // Failed to load.  This will be handled by the method itself.
            this.getLogger().warning("[FATAL] Payload failed to load it's local file (payload.yml)");
        }
        if (this.local.isFirstStartup()) {
            this.getLogger().info("This is the first startup for Payload on this server instance.  Files created.");
        }
        this.getServer().getPluginManager().registerEvents(new LockListener(), this);
        this.getCommand("payload").setExecutor(this.commandHandler);
        this.getLogger().info(PayloadPlugin.format("Payload v{0} by Jonah Seguin enabled.", PayloadPlugin.get().getDescription().getVersion()));
        try {
            new PayloadAPI(this);
            this.locked = false;
        }
        catch (IllegalAccessException ex) {
            this.getLogger().warning("Payload failed to initialize API; was already created... LOCKING");
            this.locked = true;
            this.getLogger().warning("Ensure no other plugins are creating a PayloadAPI instance.  Payload has been locked.");
        }
    }

    @Override
    public void onDisable() {
        this.getLogger().info(PayloadPlugin.format("Payload v{0} by Jonah Seguin disabled.", PayloadPlugin.get().getDescription().getVersion()));
        PayloadPlugin.instance = null;
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
     * Get the Singleton instance of the {@link PayloadPlugin} class
     * @return Instance
     */
    public static PayloadPlugin get() {
        return instance;
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

    /**
     * Is the Payload plugin globally in debug (used for default error handlers and similar)
     * @return True if debug is enabled
     */
    public boolean isDebug() {
        return debug;
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
}
