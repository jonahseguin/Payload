/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.PayloadAPI;
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
import com.jonahseguin.payload.base.sync.SyncManager;
import com.jonahseguin.payload.base.sync.SyncMode;
import com.jonahseguin.payload.base.task.PayloadAutoSaveTask;
import com.jonahseguin.payload.base.task.PayloadCleanupTask;
import com.jonahseguin.payload.base.type.*;
import com.jonahseguin.payload.database.DatabaseDependent;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.sun.istack.internal.Nullable;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * The abstract backbone of all Payload cache systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
@Getter
public abstract class PayloadCache<K, X extends Payload<K>, D extends PayloadData> implements DatabaseDependent, Comparable<PayloadCache> {

    protected final Plugin plugin; // The Bukkit JavaPlugin that created this cache.  non-persistent
    protected final PayloadPlugin payloadPlugin;
    protected final PayloadAPI api;

    protected String name; // The name for this payload cache
    protected final PayloadLangController langController;
    protected final ExecutorService pool = Executors.newCachedThreadPool();
    protected final PayloadTaskExecutor<K, X, D> executor;
    protected final CacheState<K, X, D> state;
    protected final LayerController<K, X, D> layerController = new LayerController<>();
    protected final FailureManager<K, X, D> failureManager = new FailureManager<>(this);
    protected final PayloadAutoSaveTask<K, X, D> autoSaveTask = new PayloadAutoSaveTask<>(this);
    protected final PayloadCleanupTask<K, X, D> cleanupTask = new PayloadCleanupTask<>(this);
    protected final SyncManager<K, X, D> syncManager = new SyncManager<>(this);
    protected final Class<K> keyType;
    protected final Class<X> payloadClass;
    protected boolean debug = false; // Debug for this cache
    protected Set<String> dependingCaches = new HashSet<>();
    protected PayloadErrorHandler errorHandler = new DefaultErrorHandler();
    protected PayloadDatabase payloadDatabase = null;
    protected PayloadMode mode = PayloadMode.STANDALONE; // Payload Mode for this cache
    protected SyncMode syncMode = SyncMode.UPDATE;
    protected PayloadInstantiator<X, D> instantiator = new NullPayloadInstantiator<>();
    protected boolean running = false;

    /**
     * Creates an instance of a PayloadCache
     * This constructor should ONLY be used internally by Payload
     * @param name The name of the cache.  Must be unique with no spaces or special characters (used in redis + mongo)
     * @param keyType The key type for this cache.  This is defined within the different cache implementations
     * @param payloadClass The class type for the object you will be caching.
     */
    public PayloadCache(final Plugin plugin, final PayloadPlugin payloadPlugin, final PayloadAPI api, final String name, Class<K> keyType, Class<X> payloadClass) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(payloadPlugin);
        Preconditions.checkNotNull(api);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(keyType);
        Preconditions.checkNotNull(payloadClass);
        this.keyType = keyType;
        this.payloadClass = payloadClass;
        this.plugin = plugin;
        this.payloadPlugin = payloadPlugin;
        this.api = api;
        this.name = name;
        this.executor = new PayloadTaskExecutor<>(this);
        this.state = new CacheState<>(this);
        this.langController = new PayloadLangController(payloadPlugin);

        this.langController.loadFromFile(name.toLowerCase().replaceAll(" ", "_") + ".yml");
    }

    /**
     * Internal method to convert a String to the key type (i.e UUID or String)
     * @param key String key
     * @return K key type object
     */
    public abstract K keyFromString(String key);

    /**
     * Provide the instantiator for the creation of NEW (never joined before) profiles/objects
     * This is required to be set before calling {@link #start()}
     * @param instantiator {@link PayloadInstantiator}
     */
    public void withInstantiator(PayloadInstantiator<X, D> instantiator) {
        this.instantiator = instantiator;
    }

    /**
     * Provide a custom error handler implementation for the handling of error/debug/exception messages within
     * Payload.  The default error handler should be sufficient for most use-cases,
     * in the event you want to use a library like Sentry.io, I recommend simply extending the {@link DefaultErrorHandler} class
     * and keeping the super() calls in each method, and simply adding sentry calls.
     * @param errorHandler {@link PayloadErrorHandler}
     */
    public void setErrorHandler(PayloadErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Check if the cache is locked (joinable?)
     *
     * @return Boolean locked
     */
    public final boolean isLocked() {
        return this.state.isLocked() || payloadPlugin.isLocked();
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
        if (this.getSettings().isEnableSync()) {
            this.syncManager.startup();
        }
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
        if (this.getSettings().isEnableSync()) {
            this.syncManager.shutdown();
        }
        this.shutdownPool();
        this.running = false;
        if (failedSaves > 0) {
            this.getErrorHandler().error(this, failedSaves + " Payload objects failed to save during shutdown");
            return false;
        }
        return true;
    }

    /**
     * Internal method to safely shutdown the internal cache thread pool.
     * This allows time for processes to finish executing before continuing.
     */
    private void shutdownPool() {
        try {
            this.pool.shutdown();
            this.pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            this.errorHandler.exception(this, ex, "Interrupted during shutdown of cache's thread pool");
        } finally {
            this.pool.shutdownNow();
        }
    }

    /**
     * Pass the database object for this cache.
     * Called internally by Payload
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
    @Nullable
    protected abstract X get(K key);

    /**
     * Get an object stored in this cache locally
     *
     * @param key The key to use to get the object
     * @return The object if available (else null)
     */
    @Nullable
    public abstract X getFromCache(K key);

    /**
     * Get an object stored in the first available database layer
     * @param key The key to use to get the object
     * @return The object if available (else null)
     */
    @Nullable
    public abstract X getFromDatabase(K key);

    /**
     * Check if an object is locally-cached
     * @param key Key
     * @return True if cached
     */
    public abstract boolean isCached(K key);

    /**
     * Remove an object from the local cache
     * If sync is enabled, this will also publish an UNCACHE event for other instances on this database.
     * @param key The key for the object to remove (identifier)
     * @return True if removed, otherwise false
     */
    public abstract boolean uncache(K key);

    /**
     * Remove an object from the local cache
     * Contrary to {@link #uncache(Object)}
     * @param key The key for the object to remove (identifier)
     * @return True if removed, otherwise false
     */
    public abstract boolean uncacheLocal(K key);

    /**
     * Remove an object from the local cache on ALL SERVERS with this cache in the network/on this database
     *
     * @param key Key for the object to remove (identifier)
     */
    public void uncacheEverywhere(K key) {
        if (this.getSettings().isEnableSync()) {
            this.uncacheLocal(key);
            this.syncManager.publishUncache(key);
        } else {
            this.getErrorHandler().exception(this, new UnsupportedOperationException("Cannot uncacheEverywhere unless Sync is enabled in cache settings!"));
        }
    }

    /**
     * Prepare an update for a payload
     * You can use this functionality to prevent data loss when objects are loaded across multiple instances
     * You should use this method to acquire the latest version of a payload from the server that is most relevant
     * ** This is only supported if sync is enabled {@link CacheSettings#isEnableSync()}
     * THIS IS A SYNC OPERATION.  FOR ASYNC, USE
     * @param payload The payload to get an updated version of
     * @param callback A callback with the updated Payload, you should use the callback's provided Payload parameter
     *                 to make your changes to, rather than the {@param payload} Payload you provided.
     */
    public void prepareUpdate(X payload, PayloadCallback<X> callback) {
        if (this.getSettings().isEnableSync()) {
            this.syncManager.prepareUpdate(payload, callback);
        } else {
            this.getErrorHandler().exception(this, new UnsupportedOperationException("Cannot prepareUpdate unless Sync is enabled in cache settings!"));
        }
    }

    /**
     * Async version of {@link #prepareUpdate(Payload, PayloadCallback)}
     * Simply uses the cache's thread executor service to complete this operation.
     * @see #prepareUpdate(Payload, PayloadCallback)
     * @param payload The payload to get an updated version of
     * @param callback A callback with the updated Payload, you should use the callback's provided Payload parameter
     *                 to make your changes to, rather than the {@param payload} Payload you provided.
     */
    public void prepareUpdateAsync(X payload, PayloadCallback<X> callback) {
        this.runAsync(() -> this.prepareUpdate(payload, callback));
    }

    /**
     * Get a number of objects currently stored locally in this cache
     * @return long number of objects cached
     */
    public abstract long cachedObjectCount();

    /**
     * Save a Payload to all layers / in this cache
     * If sync is enabled, this save will also propagate to other caches on the database
     * Your {@link SyncMode} setting for this cache {@link #syncMode} will determine the policy
     * for when an object is cached locally from an update/save operation.
     * {@link SyncMode#UPDATE}: Only cache if already previously cached
     * {@link SyncMode#CACHE_ALL}: Always cache
     * @param payload Payload to save
     * @return Boolean successful
     */
    public abstract boolean save(X payload);

    /**
     * Same as {@link #save(Payload)}, without the sync operations.  This is primarily used internally.
     * @param payload Payload to save
     * @return Boolean successful
     */
    public abstract boolean saveNoSync(X payload);

    /**
     * Same as {@link #save(Payload)}, but called asynchronously using the local cache thread executor pool.
     * For safety, we save the Payload to the local cache synchronously.
     * @param payload Payload to save
     * @return A {@link Future<X>} with the saved {@param payload} after the save operation is completed, successfully or not
     */
    public abstract Future<X> saveAsync(X payload);

    /**
     * Save a Payload to the local cache, without any kind of sync operation checking/etc.
     * This is used primarily internally by Payload and is NOT recommended for outside use.
     * The other {@link #cache(Payload)} is better fit for your use, as it will automatically
     * replace existing payload object instances without messing up your existing references
     * in the case that it is already cached.
     * @param payload the Payload to save
     */
    public abstract void saveToLocal(X payload);

    /**
     * Save all locally-cached objects (or for profiles, only players who are online) to the database
     * @return int : the number of failures
     */
    public abstract int saveAll();

    /**
     * Delete a Payload from all layers (including local + database)
     * If sync is enabled, the Payload will also be uncached from all other caches on this database.
     * @param key Key of payload to delete
     */
    public abstract void delete(K key);

    /**
     * Cache a Payload locally ONLY
     * I.e save to local layer
     *
     * @param payload Payload to cache (save locally)
     */
    public abstract void cache(X payload);


    /**
     * Load all database-stored objects into the local cache
     */
    public abstract void cacheAll();

    /**
     * Get all objects across all layers
     * @return A HashSet containing all the objects
     */
    public abstract Set<X> getAll();

    /**
     * Get all currently locally-cached objects
     * @return Locally-cached objects
     */
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

    /**
     * Internal method used by payload to provide a server-specific name for this cache, if server-specific caching is enabled.
     * This is primarily used by Redis layers for naming the redis key.
     * @return String: The server specific name for this cache
     */
    public final String getServerSpecificName() {
        if (this.getSettings().isServerSpecific()) {
            return api.getPayloadID() + "-" + this.getName();
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

    /**
     * Set the SyncMode for this cache
     * The sync mode determines the policy for when to cache objects that are saved in other servers
     *
     * Your {@link SyncMode} setting for this cache {@link #syncMode} will determine the policy
     * for when an object is cached locally from an update/save operation.
     * {@link SyncMode#UPDATE}: Only cache if already previously cached
     * {@link SyncMode#CACHE_ALL}: Always cache
     *
     * @param mode {@link SyncMode}
     */
    public void setSyncMode(SyncMode mode) {
        this.syncMode = mode;
    }

    /**
     * Internal method used by Payload to forcefully update a local instance of a Payload object with a newer one,
     * allowing your references to the existing Payload to remain intact and up-to-date.
     * Note that this only effects persistent (non-transient) fields.
     * @param payload The Payload to update
     * @param update The newer version of said payload to replace the values of {@param payload} with.
     */
    public void updatePayloadFromNewer(X payload, X update) {
        this.payloadDatabase.getDatastore().getMapper().getMappedClass(this.payloadClass).getPersistenceFields().forEach(mf -> {
            mf.setFieldValue(payload, mf.getFieldValue(update));
        });
    }

    /**
     * Internal method that handles when MongoDB disconnects
     */
    @Override
    public void onMongoDbDisconnect() {
        this.getPayloadDatabase().getState().setMongoConnected(false);
    }

    /**
     * Internal method that handles when Redis disconnects
     */
    @Override
    public void onRedisDisconnect() {
        this.getPayloadDatabase().getState().setRedisConnected(false);
    }

    /**
     * Internal method that handles when a MongoDB connection is established after a previous disconnect
     */
    @Override
    public void onMongoDbReconnect() {
        this.getPayloadDatabase().getState().setMongoConnected(true);
    }

    /**
     * Internal method that handles when a Redis connection is established after a previous disconnect
     */
    @Override
    public void onRedisReconnect() {
        this.getPayloadDatabase().getState().setRedisConnected(true);
    }

    /**
     * Internal method that handles when MongoDB connects for the first time
     */
    @Override
    public void onMongoDbInitConnect() {
        this.getPayloadDatabase().getState().setMongoConnected(true);
        this.getPayloadDatabase().getState().setMongoInitConnect(true);
        if (this.getPayloadDatabase().getState().isDatabaseConnected()) {
            // Both connected
            this.getState().unlock();
        }
    }

    /**
     * Internal method that handles when Redis connects for the first time
     */
    @Override
    public void onRedisInitConnect() {
        this.getPayloadDatabase().getState().setRedisConnected(true);
        this.getPayloadDatabase().getState().setRedisInitConnect(true);
    }

    /**
     * Utility method to send a message to online players with a certain permission
     * @param required The required permission
     * @param lang The language definition
     * @param args The arguments for the language definition
     */
    public void alert(PayloadPermission required, PLang lang, String... args) {
        Bukkit.getLogger().info(this.langController.get(lang, args));
        for (Player pl : this.plugin.getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(this.langController.get(lang, args));
            }
        }
    }

    /**
     * Utility method to send a message to online players with a certain permission
     * @param required The required permission
     * @param msg The message to send
     */
    public void alert(PayloadPermission required, String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        Bukkit.getLogger().info(msg);
        for (Player pl : this.plugin.getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(msg);
            }
        }
    }

    /**
     * Simple utility method to run a task asynchronously in a separate thread provided by the cache's local cached thread executor pool.
     * This is recommended over using the Bukkit scheduler when performing operations relative to the cache, as it will ensure operations
     * are completed BEFORE cache shutdown, plus the cached thread nature yields a slight performance improvement.
     * @see Executors#newCachedThreadPool()
     * @param runnable The task to run
     */
    public void runAsync(Runnable runnable) {
        this.pool.submit(runnable);
    }

    /**
     * Simple utility method to run a task asynchronously in a separate thread provided by the cache's local cached thread executor pool.
     * This is recommended over using the Bukkit scheduler when performing operations relative to the cache, as it will ensure operations
     * are completed BEFORE cache shutdown, plus the cached thread nature yields a slight performance improvement.
     * @see Executors#newCachedThreadPool()
     * @param callable The task to run
     * @return {@link Future<T>} a future with the callable's parameter after execution has completed.
     */
    public <T> Future<T> runAsync(Callable<T> callable) {
        return this.pool.submit(callable);
    }

    /**
     * Internal method to update the Payload ID for objects stored in this cache after the Payload ID has been changed
     * (via command)
     */
    public abstract void updatePayloadID();

    /**
     * Add a dependency to this cache
     * Dependencies of this cache will:
     * - Cache objects before this cache (primarily for profiles)
     * - Initialize objects before this cache
     * @param cache The {@link PayloadCache} implementation for this cache to depend on.
     */
    public void addDepend(PayloadCache cache) {
        this.dependingCaches.add(cache.getName());
    }

    /**
     * Checks if this cache is dependent on a specific cache.
     * This is used primarily internally for determining the loading order when sorting caches during
     * initializing/loading.
     * @param cache {@link PayloadCache}
     * @return True if this cache is dependent, false if it's not
     */
    public boolean isDependentOn(PayloadCache cache) {
        return this.dependingCaches.contains(cache.getName());
    }

    /**
     * Simple comparator method to determine order between caches based on dependencies
     * @param o The {@link PayloadCache} to compare.
     * @return Comparator sorting integer
     */
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
