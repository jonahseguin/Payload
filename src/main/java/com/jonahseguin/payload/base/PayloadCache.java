/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.error.DefaultErrorHandler;
import com.jonahseguin.payload.base.error.PayloadErrorHandler;
import com.jonahseguin.payload.base.exception.runtime.PayloadRuntimeException;
import com.jonahseguin.payload.base.failsafe.FailureManager;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.lang.PayloadLangController;
import com.jonahseguin.payload.base.layer.LayerController;
import com.jonahseguin.payload.base.settings.CacheSettings;
import com.jonahseguin.payload.base.state.CacheState;
import com.jonahseguin.payload.base.state.PayloadTaskExecutor;
import com.jonahseguin.payload.base.task.PayloadAutoSaveTask;
import com.jonahseguin.payload.base.task.PayloadCleanupTask;
import com.jonahseguin.payload.base.type.*;
import com.jonahseguin.payload.database.DatabaseDependent;
import com.jonahseguin.payload.database.PayloadDatabase;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The abstract backbone of all Payload cache systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
@Getter
public abstract class PayloadCache<K, X extends Payload, D extends PayloadData> implements DatabaseDependent, Comparable<PayloadCache> {

    protected final transient Plugin plugin; // The Bukkit JavaPlugin that created this cache.  non-persistent

    protected String name; // The name for this payload cache

    protected transient boolean debug = false; // Debug for this cache

    protected transient Set<String> dependingCaches = new HashSet<>();
    protected transient PayloadErrorHandler errorHandler = new DefaultErrorHandler();
    protected transient PayloadDatabase payloadDatabase = null;
    protected transient PayloadMode mode = PayloadMode.STANDALONE; // Payload Mode for this cache

    protected transient final ExecutorService pool = Executors.newCachedThreadPool();
    protected transient final PayloadTaskExecutor<K, X, D> executor;
    protected transient final PayloadLangController langController = new PayloadLangController();
    protected transient final CacheState<K, X, D> state;
    protected transient final LayerController<K, X, D> layerController = new LayerController<>();
    protected transient final FailureManager<K, X, D> failureManager = new FailureManager<>(this);
    protected transient final PayloadAutoSaveTask<K, X, D> autoSaveTask = new PayloadAutoSaveTask<>(this);
    protected transient final PayloadCleanupTask<K, X, D> cleanupTask = new PayloadCleanupTask<>(this);
    protected transient PayloadInstantiator<X, D> instantiator = new NullPayloadInstantiator<>();

    protected transient final Class<K> keyType;
    protected transient final Class<X> payloadClass;

    protected transient boolean running = false;

    public PayloadCache(final PayloadHook hook, final String name, Class<K> keyType, Class<X> payloadClass) {
        if (hook.getPlugin() == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (hook.getPlugin() instanceof PayloadPlugin) {
            throw new IllegalArgumentException("Plugin cannot be PayloadPlugin");
        }
        if (!hook.isValid()) {
            throw new IllegalStateException("Provided PayloadHook is not valid; cannot create cache '" + name + "'");
        }
        this.keyType = keyType;
        this.payloadClass = payloadClass;
        this.plugin = hook.getPlugin();
        this.name = name;
        this.executor = new PayloadTaskExecutor<>(this);
        this.state = new CacheState<>(this);

        this.langController.loadFromFile(name.toLowerCase().replaceAll(" ", "_") + ".yml");
    }

    public void withInstantiator(PayloadInstantiator<X, D> instantiator) {
        this.instantiator = instantiator;
    }

    public void setErrorHandler(PayloadErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Check if the cache is locked (joinable?)
     *
     * @return Boolean locked
     */
    public final boolean isLocked() {
        return this.state.isLocked() || PayloadPlugin.get().isLocked();
    }

    /**
     * Start the Cache
     * Should be called by the external plugin during startup after the cache has been created
     * @return Boolean successful
     */
    public final boolean start() {
        if (this.isRunning()) return true;
        if (this.instantiator instanceof NullPayloadInstantiator) {
            this.getState().lock();
            throw new PayloadRuntimeException("Instantiator for cache " + this.getName() + " cannot be Null!  Call withInstantiator() before starting!");
        }
        this.init();
        this.running = true;
        this.failureManager.start();
        this.autoSaveTask.start();
        this.cleanupTask.start();
        return true;
    }

    /**
     * Stop the Cache
     * Should be called by the external plugin during shutdown
     * @return Boolean successful
     */
    public final boolean stop() {
        if (!this.isRunning()) return true;

        int failedSaves = this.saveAll(); // First, save everything.

        this.shutdown(); // Allow the implementing cache to do it's shutdown first

        this.autoSaveTask.stop();
        this.cleanupTask.stop();
        this.failureManager.stop();
        this.pool.shutdown(); // Shutdown our thread pool
        this.running = false;
        if (failedSaves > 0) {
            this.getErrorHandler().error(this, failedSaves + " Payload objects failed to save during shutdown");
            return false;
        }
        return true;
    }

    /**
     * Pass the database object for this cache.
     * Called internally.
     *
     * @param database PayloadDatabase
     */
    public final void setupDatabase(PayloadDatabase database) {
        if (this.payloadDatabase != null) {
            throw new IllegalStateException("Database has already been defined");
        }
        this.payloadDatabase = database;
    }

    /**
     * Get/create a controller for specific data
     *
     * @param data {@link PayloadData}
     * @return {@link PayloadController}
     */
    public abstract PayloadController<X> controller(D data);

    /**
     * Get the Cache Settings for this Cache
     *
     * @return Cache Settings
     */
    public abstract CacheSettings getSettings();

    /**
     * Starts up & initializes the cache.
     * Prepares everything for a fresh startup, ensures database connections, etc.
     */
    protected abstract void init();

    /**
     * Shut down the cache.
     * Saves everything first, and safely shuts down
     */
    protected abstract void shutdown();


    /**
     * Get an object stored in this cache, using the best method provided by the cache
     *
     * @param key The key to use to get the object (i.e a string, number, etc.)
     * @return The object if available (else null)
     */
    protected abstract X get(K key);

    /**
     * Get a number of objects currently stored locally in this cache
     * @return long number of objects cached
     */
    public abstract long cachedObjectCount();

    /**
     * Save a Payload to all layers / in this cache
     * @param payload Payload to save
     * @return Boolean successful
     */
    public abstract boolean save(X payload);

    /**
     * Cache a Payload locally ONLY
     * I.e save to local layer
     *
     * @param payload Payload to cache (save locally)
     */
    public abstract void cache(X payload);

    public abstract int saveAll();

    public abstract Collection<X> getCachedObjects();

    /**
     * Get the name of this cache (set by the end user, should be unique)
     * A {@link com.jonahseguin.payload.base.exception.DuplicateCacheException} error will be thrown if another cache
     * exists with the same name or ID during creation.
     *
     * @return String: Cache Name
     */
    public final String getName() {
        return this.name;
    }

    public final String getServerSpecificName() {
        if (this.getSettings().isServerSpecific()) {
            return PayloadAPI.get().getPayloadID() + "-" + this.getName();
        } else {
            return this.getName();
        }
    }

    /**
     * Get the JavaPlugin controlling this cache
     * Every Payload cache must be associated with a JavaPlugin for event handling and etc.
     *
     * @return Plugin
     */
    public final Plugin getPlugin() {
        return this.plugin;
    }

    /**
     * Get the current Mode this cache is functioning in
     * There are two modes: STANDALONE, or NETWORK_NODE
     * In standalone mode, a cache functions as it's own entity and will use login/logout events to handle caching normally
     * In contrast, in network node mode, a cache functions as a node in a BungeeCord/etc. proxied network,
     * where in such logins are handled before logouts, data is transferred through a handshake via Redis pub/sub if
     * a player is already logged into another node.
     *
     * @return {@link PayloadMode} the current mode
     */
    public PayloadMode getMode() {
        return mode;
    }

    /**
     * Set the current Mode this cache is functioning in
     * Two modes: STANDALONE, or NETWORK_NODE
     *
     * @param mode {@link PayloadMode} mode
     * @see #getMode()
     */
    public void setMode(PayloadMode mode) {
        this.mode = mode;
    }

    @Override
    public void onMongoDbDisconnect() {
        this.getPayloadDatabase().getState().setMongoConnected(false);
    }

    @Override
    public void onRedisDisconnect() {
        this.getPayloadDatabase().getState().setRedisConnected(false);
    }

    @Override
    public void onMongoDbReconnect() {
        this.getPayloadDatabase().getState().setMongoConnected(true);
    }

    @Override
    public void onRedisReconnect() {
        this.getPayloadDatabase().getState().setRedisConnected(true);
    }

    @Override
    public void onMongoDbInitConnect() {
        this.getPayloadDatabase().getState().setMongoConnected(true);
        this.getPayloadDatabase().getState().setMongoInitConnect(true);
        if (this.getPayloadDatabase().getState().isDatabaseConnected()) {
            // Both connected
            this.getState().unlock();
        }
    }

    @Override
    public void onRedisInitConnect() {
        this.getPayloadDatabase().getState().setRedisConnected(true);
        this.getPayloadDatabase().getState().setRedisInitConnect(true);
    }

    public void alert(PayloadPermission required, PLang lang, String... args) {
        Bukkit.getLogger().info(this.langController.get(lang, args));
        for (Player pl : this.plugin.getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(this.langController.get(lang, args));
            }
        }
    }

    public void alert(PayloadPermission required, String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        Bukkit.getLogger().info(msg);
        for (Player pl : this.plugin.getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(msg);
            }
        }
    }

    public void runAsync(Runnable runnable) {
        this.pool.submit(runnable);
    }

    public <T> Future<T> runAsync(Callable<T> callable) {
        return this.pool.submit(callable);
    }

    public abstract void updatePayloadID();

    public void addDepend(PayloadCache cache) {
        this.dependingCaches.add(cache.getName());
    }

    public boolean isDependentOn(PayloadCache cache) {
        return this.dependingCaches.contains(cache.getName());
    }

    @Override
    public int compareTo(PayloadCache o) {
        if (this.isDependentOn(o)) {
            return -1;
        } else if (o.isDependentOn(this)) {
            return 1;
        } else {
            return 0;
        }
    }
}
