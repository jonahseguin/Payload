/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.base.failsafe.FailureManager;
import com.jonahseguin.payload.base.handshake.HandshakeService;
import com.jonahseguin.payload.base.lang.LangService;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.network.NetworkService;
import com.jonahseguin.payload.base.sync.SyncService;
import com.jonahseguin.payload.base.task.PayloadAutoSaveTask;
import com.jonahseguin.payload.base.type.*;
import com.jonahseguin.payload.database.DatabaseDependent;
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.server.ServerService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

/**
 * The abstract backbone of all Payload cache systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
@Getter
public abstract class PayloadCache<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> implements DatabaseDependent, Comparable<PayloadCache>, PayloadCacheService<K, X, N, D> {

    protected final String name;
    protected final Class<K> keyType;
    protected final Class<X> payloadClass;
    protected final Injector injector;
    protected final ExecutorService pool = Executors.newCachedThreadPool();
    protected final FailureManager<K, X, N, D> failureManager;
    protected final PayloadAutoSaveTask<K, X, N, D> autoSaveTask = new PayloadAutoSaveTask<>(this);
    protected final Set<String> dependingCaches = new HashSet<>();
    @Inject
    protected Plugin plugin;
    @Inject
    protected PayloadPlugin payloadPlugin;
    @Inject
    protected PayloadAPI api;
    @Inject
    protected DatabaseService database;
    @Inject
    protected LangService lang;
    @Inject
    protected ErrorService errorService;
    @Inject
    protected PayloadInstantiator<K, X, D> instantiator;
    @Inject
    protected SyncService<K, X, N, D> sync;
    @Inject
    protected HandshakeService handshakeService;
    @Inject
    protected NetworkService<K, X, N, D> networkService;
    @Inject
    protected ServerService serverService;
    protected boolean debug = false;
    protected PayloadMode mode = PayloadMode.STANDALONE;
    protected boolean running = false;

    /**
     * Creates an instance of a PayloadCache
     * This constructor should ONLY be used internally by Payload
     */
    public PayloadCache(@Nonnull Injector injector, @Nonnull String name, @Nonnull Class<K> keyType, @Nonnull Class<X> payloadClass) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(payloadPlugin);
        Preconditions.checkNotNull(api);
        Preconditions.checkNotNull(injector);
        Preconditions.checkNotNull(database);
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(keyType);
        Preconditions.checkNotNull(payloadClass);
        this.injector = injector.createChildInjector(module());
        this.injector.injectMembers(this);
        this.name = name;
        this.keyType = keyType;
        this.payloadClass = payloadClass;
        this.failureManager = new FailureManager<>(this, payloadPlugin);
    }

    @Nonnull
    protected abstract CacheModule<K, X, N, D> module();

    /**
     * Provide the instantiator for the creation of NEW (never joined before) profiles/objects
     * The default value is {@link GuicePayloadInstantiator}.  You can use your own if you deem necessary.
     * @param instantiator {@link PayloadInstantiator}
     */
    @Override
    public final void setInstantiator(@Nonnull PayloadInstantiator<K, X, D> instantiator) {
        Preconditions.checkNotNull(instantiator);
        this.instantiator = instantiator;
    }

    /**
     * Start the Cache
     * Should be called by the external plugin during startup after the cache has been created
     * @return Boolean successful
     */
    @Override
    public final boolean start() {
        if (running) return true;
        Preconditions.checkNotNull(instantiator, "Instantiator must be set before calling start()");
        Preconditions.checkState(database.isRunning(), "Database must be started before starting cache");
        boolean success = true;
        if (!initialize()) {
            success = false;
        }
        if (!handshakeService.start()) {
            success = false;
        }
        if (getMode().equals(PayloadMode.NETWORK_NODE)) {
            if (!networkService.start()) {
                success = false;
            }
        }
        if (!serverService.start()) {
            success = false;
        }
        failureManager.start();
        autoSaveTask.start();
        if (getSettings().isEnableSync()) {
            if (!sync.start()) {
                success = false;
            }
        }
        running = true;
        return success;
    }

    /**
     * Stop the Cache
     * Should be called by the external plugin during shutdown
     * @return Boolean successful
     */
    public final boolean shutdown() {
        if (!running) return true;

        int failedSaves = saveAll(); // First, save everything.

        boolean success = true;

        if (!shutdown()) {
            success = false;
        }

        autoSaveTask.stop();
        failureManager.stop();
        if (!handshakeService.shutdown()) {
            success = false;
        }
        if (getMode().equals(PayloadMode.NETWORK_NODE)) {
            if (!networkService.shutdown()) {
                success = false;
            }
        }
        if (!serverService.shutdown()) {
            success = false;
        }
        if (getSettings().isEnableSync()) {
            this.sync.shutdown();
        }
        shutdownPool();
        running = false;
        if (failedSaves > 0) {
            errorService.capture(failedSaves + " Payload objects failed to save during shutdown");
            success = false;
        }
        return success;
    }

    /**
     * Internal method to safely shutdown the internal cache thread pool.
     * This allows time for processes to finish executing before continuing.
     */
    private void shutdownPool() {
        try {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            errorService.capture(ex, "Interrupted during shutdown of cache's thread pool");
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * Get/create a controller for specific data
     *
     * @param data {@link PayloadData}
     * @return {@link PayloadController}
     */
    public abstract PayloadController<X> controller(@Nonnull D data);

    public abstract PayloadController<X> controller(@Nonnull K key);

    /**
     * Starts up & initializes the cache.
     * Prepares everything for a fresh startup, ensures database connections, etc.
     */
    protected abstract boolean initialize();

    /**
     * Shut down the cache.
     * Saves everything first, and safely shuts down
     */
    protected abstract boolean terminate();

    /**
     * Get a number of objects currently stored locally in this cache
     * @return long number of objects cached
     */
    public abstract long cachedObjectCount();

    /**
     * Get the name of this cache (set by the end user, should be unique)
     * A {@link com.jonahseguin.payload.base.exception.DuplicateCacheException} error will be thrown if another cache
     * exists with the same name or ID during creation.
     *
     * @return String: Cache Name
     */
    @Nonnull
    @Override
    public final String getName() {
        return name;
    }

    /**
     * Internal method used by payload to provide a server-specific name for this cache, if server-specific caching is enabled.
     * This is primarily used by Redis layers for naming the redis key.
     * @return String: The server specific name for this cache
     */
    @Nonnull
    @Override
    public final String getServerSpecificName() {
        if (getSettings().isServerSpecific()) {
            return api.getPayloadID() + "-" + name;
        } else {
            return name;
        }
    }

    /**
     * Get the JavaPlugin controlling this cache
     * Every Payload cache must be associated with a JavaPlugin for event handling and etc.
     *
     * @return Plugin
     */
    @Nonnull
    public final Plugin getPlugin() {
        return plugin;
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
    @Nonnull
    public final PayloadMode getMode() {
        return mode;
    }

    /**
     * Set the current Mode this cache is functioning in
     * Two modes: STANDALONE, or NETWORK_NODE
     *
     * @param mode {@link PayloadMode} mode
     * @see #getMode()
     */
    @Override
    public void setMode(@Nonnull PayloadMode mode) {
        Preconditions.checkNotNull(mode);
        this.mode = mode;
    }
    /**
     * Internal method used by Payload to forcefully update a local instance of a Payload object with a newer one,
     * allowing your references to the existing Payload to remain intact and up-to-date.
     * Note that this only effects persistent (non-transient) fields.
     * @param payload The Payload to update
     * @param update The newer version of said payload to replace the values of {@param payload} with.
     */
    protected final void updatePayloadFromNewer(@Nonnull X payload, @Nonnull X update) {
        Preconditions.checkNotNull(payload);
        Preconditions.checkNotNull(update);
        database.getDatastore().getMapper().getMappedClass(payloadClass).getPersistenceFields().forEach(mf -> {
            mf.setFieldValue(payload, mf.getFieldValue(update));
        });
    }

    @Override
    public Optional<N> getNetworked(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        return networkService.get(key);
    }

    @Override
    public Optional<N> getNetworked(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        return networkService.get(payload);
    }

    @Override
    public Optional<X> get(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        return controller(key).cache();
    }

    @Override
    public Future<Optional<X>> getAsync(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        return runAsync(() -> get(key));
    }

    @Override
    public Optional<X> getFromCache(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        return getLocalStore().get(key);
    }

    @Override
    public Optional<X> getFromDatabase(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        return getDatabaseStore().get(key);
    }

    @Override
    public boolean save(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        if (saveNoSync(payload)) {
            if (getSettings().isEnableSync()) {
                sync.update(payload.getIdentifier());
            }
            return true;
        }
        return false;
    }

    @Override
    public Future<Boolean> saveAsync(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        return runAsync(() -> save(payload));
    }

    @Override
    public void cache(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        Optional<X> o = getLocalStore().get(payload.getIdentifier());
        if (o.isPresent()) {
            updatePayloadFromNewer(o.get(), payload);
        } else {
            getLocalStore().save(payload);
        }
    }

    @Override
    public void uncache(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        getLocalStore().remove(key);
    }

    @Override
    public void uncache(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        getLocalStore().remove(payload);
    }

    @Override
    public void delete(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        getLocalStore().remove(key);
        getDatabaseStore().remove(key);
    }

    @Override
    public void delete(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        getLocalStore().remove(payload);
        getDatabaseStore().remove(payload);
    }

    @Override
    public boolean isCached(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        return getLocalStore().has(key);
    }

    @Override
    public void prepareUpdate(@Nonnull X payload, @Nonnull PayloadCallback<X> callback) {
        Preconditions.checkState(getSettings().isEnableSync(), "Cannot prepare update when sync is disabled!");
        Preconditions.checkNotNull(payload);
        Preconditions.checkNotNull(callback);
        sync.prepareUpdate(payload, callback);
    }

    @Override
    public void prepareUpdateAsync(@Nonnull X payload, @Nonnull PayloadCallback<X> callback) {
        Preconditions.checkState(getSettings().isEnableSync(), "Cannot prepare update when sync is disabled!");
        Preconditions.checkNotNull(payload);
        Preconditions.checkNotNull(callback);
        runAsync(() -> sync.prepareUpdate(payload, callback));
    }

    @Override
    public void cacheAll() {
        getDatabaseStore().getAll().forEach(this::cache);
    }

    @Nonnull
    @Override
    public SyncService<K, X, N, D> getSyncService() {
        return sync;
    }

    /**
     * Utility method to send a message to online players with a certain permission
     * @param required The required permission
     * @param module The language module
     * @param key The language definition
     * @param args The arguments for the language definition
     */
    public void alert(@Nonnull PayloadPermission required, @Nonnull String module, @Nonnull String key, @Nullable Object... args) {
        Preconditions.checkNotNull(required);
        Preconditions.checkNotNull(module);
        Preconditions.checkNotNull(key);
        plugin.getLogger().info(lang.module(module).format(key, args));
        for (Player pl : plugin.getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(lang.module(module).format(key, args));
            }
        }
    }

    /**
     * Utility method to send a message to online players with a certain permission
     * @param required The required permission
     * @param msg The message to send
     */
    public void alert(@Nonnull PayloadPermission required, @Nonnull String msg) {
        Preconditions.checkNotNull(required);
        Preconditions.checkNotNull(msg);
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        Bukkit.getLogger().info(msg);
        for (Player pl : plugin.getServer().getOnlinePlayers()) {
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
    public void runAsync(@Nonnull Runnable runnable) {
        Preconditions.checkNotNull(runnable);
        pool.submit(runnable);
    }

    /**
     * Simple utility method to run a task asynchronously in a separate thread provided by the cache's local cached thread executor pool.
     * This is recommended over using the Bukkit scheduler when performing operations relative to the cache, as it will ensure operations
     * are completed BEFORE cache shutdown, plus the cached thread nature yields a slight performance improvement.
     * @see Executors#newCachedThreadPool()
     * @param callable The task to run
     * @return {@link Future<T>} a future with the callable's parameter after execution has completed.
     */
    @Nonnull
    public <T> Future<T> runAsync(@Nonnull Callable<T> callable) {
        Preconditions.checkNotNull(callable);
        return pool.submit(callable);
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
    @Override
    public void addDepend(@Nonnull PayloadCache cache) {
        Preconditions.checkNotNull(cache);
        this.dependingCaches.add(cache.getName());
    }

    /**
     * Checks if this cache is dependent on a specific cache.
     * This is used primarily internally for determining the loading order when sorting caches during
     * initializing/loading.
     * @param cache {@link PayloadCache}
     * @return True if this cache is dependent, false if it's not
     */
    @Override
    public boolean isDependentOn(@Nonnull PayloadCache cache) {
        Preconditions.checkNotNull(cache);
        return dependingCaches.contains(cache.getName());
    }

    /**
     * Simple comparator method to determine order between caches based on dependencies
     * @param o The {@link PayloadCache} to compare.
     * @return Comparator sorting integer
     */
    @Override
    public int compareTo(@Nonnull PayloadCache o) {
        Preconditions.checkNotNull(o);
        if (this.isDependentOn(o)) {
            return -1;
        } else if (o.isDependentOn(this)) {
            return 1;
        } else {
            return 0;
        }
    }
}
