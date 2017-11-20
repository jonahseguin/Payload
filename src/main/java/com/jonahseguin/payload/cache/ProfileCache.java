package com.jonahseguin.payload.cache;

import com.jonahseguin.payload.Payload;
import com.jonahseguin.payload.caching.CachingController;
import com.jonahseguin.payload.caching.LayerExecutorHandler;
import com.jonahseguin.payload.event.AsyncCacheLoadFinishEvent;
import com.jonahseguin.payload.event.AsyncCacheLoadStartEvent;
import com.jonahseguin.payload.event.ProfileCacheListener;
import com.jonahseguin.payload.fail.CacheFailureHandler;
import com.jonahseguin.payload.layers.*;
import com.jonahseguin.payload.profile.*;
import com.jonahseguin.payload.task.AfterJoinTask;
import com.jonahseguin.payload.task.CacheAutoSaveTask;
import com.jonahseguin.payload.task.CacheCleanupTask;
import com.jonahseguin.payload.type.CacheResult;
import com.jonahseguin.payload.type.CacheSettings;
import com.jonahseguin.payload.type.CacheSource;
import com.jonahseguin.payload.type.CacheStage;
import com.jonahseguin.payload.util.PayloadCallback;
import lombok.Getter;

import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

@Getter
public class ProfileCache<T extends Profile> {

    private final String cacheId = UUID.randomUUID().toString();

    public static final String FAILED_CACHE_KICK_MESSAGE = ChatColor.RED + "An error occurred while loading your profile.\n" +
            ChatColor.RED + "This is should not happen.  Try re-logging and contact an administrator.\n" +
            ChatColor.GRAY + "We apologize for the inconvenience.";

    private final PProfileCache<T> simpleCache = new PProfileCache<>(this);
    private final ConcurrentMap<String, CachingController<T>> controllers = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final Class<T> profileClass;
    private final CacheSettings<T> settings;
    private final ProfileInstantiator<T> profileInstantiator;
    private final CacheDebugger debugger;
    private final CacheDatabase database;
    private final LayerController<T> layerController;
    private final CacheFailureHandler<T> failureHandler;
    private final CacheCleanupTask cleanupTask;
    private final AfterJoinTask afterJoinTask;
    private final LayerExecutorHandler<T> executorHandler;
    private CacheAutoSaveTask cacheAutoSaveTask;
    private boolean allowJoinsMode = false; // Internal join prevention; prevent joins when setting up/shutting down, etc.
    private ProfileCacheListener<T> profileCacheListener = null;

    public ProfileCache(CacheSettings<T> settings, Class<T> clazz) {
        this.settings = settings;
        this.profileClass = clazz;
        this.plugin = settings.getPlugin();
        this.profileInstantiator = settings.getProfileInstantiator();
        this.debugger = settings.getDebugger();
        this.database = settings.getDatabase();
        this.failureHandler = new CacheFailureHandler<>(this, settings.getPlugin());
        this.executorHandler = new LayerExecutorHandler<>(this);
        this.layerController = new LayerController<>(this);
        this.cleanupTask = new CacheCleanupTask();
        this.afterJoinTask = new AfterJoinTask(this);
    }

    public final boolean init() {
        this.allowJoinsMode = false;
        boolean success = true;
        if (!layerController.init()) {
            success = false;
        }
        if (success) {
            this.afterJoinTask.startTask();
            this.allowJoinsMode = true;
            this.profileCacheListener = new ProfileCacheListener<>(this);
            this.plugin.getServer().getPluginManager().registerEvents(profileCacheListener, plugin);
            this.cacheAutoSaveTask = new CacheAutoSaveTask(this);
        } else {
            handleStartupFail();
        }
        return success;
    }

    public final boolean shutdown() {
        this.allowJoinsMode = false;
        boolean success = true;
        if (!layerController.shutdown()) {
            success = false;
        }
        if (this.afterJoinTask != null) {
            this.afterJoinTask.stopTask();
        }
        if (this.failureHandler != null) {
            this.failureHandler.getCacheFailureTask().stop();
        }
        if (this.cacheAutoSaveTask != null) {
            this.cacheAutoSaveTask.getTask().cancel();
        }
        return success;
    }

    private void handleStartupFail() {
        debugger.debug("[FATAL ERROR]  An error occurred while starting up the cache..");
        if(getDebugger().onStartupFailure()) {
            shutdown();
        }
    }

    public CachingController<T> getController(String username, String uniqueId) {
        if (this.controllers.containsKey(uniqueId)) {
            return this.controllers.get(uniqueId);
        }
        return new CachingController<>(this, new SimpleProfilePassable(uniqueId, username));
    }

    public CachingController<T> getController(Player player) {
        if (this.controllers.containsKey(player.getUniqueId().toString())) {
            return this.controllers.get(player.getUniqueId().toString());
        }
        return new CachingController<>(this, new SimpleProfilePassable(player.getUniqueId().toString(), player.getName()))
                .withPlayer(player);
    }

    public boolean hasController(String uniqueId) {
        return this.controllers.containsKey(uniqueId);
    }

    public void destroyController(String uniqueId) {
        this.controllers.remove(uniqueId);
    }

    public T getProfile(Player player) {
        // getProfile --> UUID
        return getProfile(player.getUniqueId().toString());
    }

    public T getLocalProfile(Player player) {
        if (this.localLayer.has(player.getUniqueId().toString())) {
            return this.localLayer.get(player.getUniqueId().toString());
        }
        return null;
    }

    public T getProfile(String uniqueId) {
        // Ensure the profile we get is not temporary
        // If not cached, check for PreCached [CachingProfile] or FailedCachedProfile
        // --> could mean they are BEING cached, and not yet cached
        // --> so we have to return a TEMPORARY profile
        // --> and indicate somehow to the requesting method that the Profile we return is temporary
        if (localLayer.has(uniqueId)) {
            return localLayer.get(uniqueId);
        } else if (preCachingLayer.has(uniqueId)) {
            // Being cached currently; return their temporary profile.
            return preCachingLayer.get(uniqueId).getTemporaryProfile();
        } else if (redisLayer.has(uniqueId)) {
            return redisLayer.get(uniqueId);
        } else if (mongoLayer.has(uniqueId)) {
            return mongoLayer.get(uniqueId);
        } else if (failureHandler.hasFailedProfile(uniqueId)) {
            FailedCachedProfile<T> failedCachedProfile = failureHandler.getFailedProfile(uniqueId);
            if (failedCachedProfile != null) {
                if (failedCachedProfile.getCachingProfile() != null) {
                    return failedCachedProfile.getCachingProfile().getTemporaryProfile();
                } else {
                    // They should have a caching profile... kick them
                    failedCachedProfile.tryToGetPlayer().kickPlayer(ChatColor.RED + "Your profile was not loaded [no caching profile on get].\n" +
                            ChatColor.RED + "Please notify an administrator of this error and try to re-log\n" +
                            ChatColor.GRAY + "We apologize for the inconvenience.");
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void cacheAsyncStart(String username, String uniqueId, PayloadCallback<CacheResult<T>> resultCallback) {
        // Called in AsyncPlayerPreLoginEvent when Async caching is enabled
        Payload.runASync(plugin, () -> cache(username, uniqueId,
                cachingProfile -> plugin.getServer().getPluginManager().callEvent(new AsyncCacheLoadStartEvent(cachingProfile)),
                cacheResult -> {
                    if (cacheResult.isSuccess()) {
                        if (!cacheResult.isAllowedJoin()) {
                            afterJoinTask.addTask(cacheResult.getCachingProfile(), (profile, player) -> player.kickPlayer(ProfileCache.FAILED_CACHE_KICK_MESSAGE));
                        }
                        plugin.getServer().getPluginManager().callEvent(new AsyncCacheLoadFinishEvent(this, cacheResult));
                        if (preCachingLayer.has(cacheResult.getUniqueId())) {
                            preCachingLayer.remove(cacheResult.getUniqueId());
                        }
                        this.cacheAsyncFinish(cacheResult, resultCallback);
                    } else {
                        cacheResult = failCacheResult(cacheResult.getCachingProfile(), cacheResult.getProfile(),
                                cacheResult.getUniqueId(), cacheResult.getName(), cacheResult.getCacheSource(),
                                cacheResult.isAllowedJoin());
                        resultCallback.call(cacheResult);
                    }
                }));
    }

    public CacheResult<T> cacheAsyncFinish(CacheResult<T> cacheResult, PayloadCallback<CacheResult<T>> callback) {
        // Called from AsyncCacheLoadFinishEvent after caching is finished
        final T profile = cacheResult.getProfile();
        if (profile != null) {
            if (cacheResult.getCachingProfile() != null) {
                this.afterJoinTask.addTask(cacheResult.getCachingProfile(), ((cachingProfile, player) -> {
                    if (player != null && player.isOnline()) {
                        if (!cacheResult.isAllowedJoin()) {
                            player.kickPlayer(ChatColor.RED + "Your profile failed to load.");
                        } else if (cacheResult.getProfile() != null) {
                            initProfile(player, profile);
                        }
                        callback.call(cacheResult);
                    }
                }));
            }
        }
        return cacheResult;
    }

    public CacheResult<T> cache(String username, String uniqueId, PayloadCallback<CachingProfile<T>> preCaching, PayloadCallback<CacheResult<T>> cached) {
        CachingProfile<T> cachingProfile = this.preCachingLayer.provide(new SimpleProfilePassable(uniqueId, username));
        if (cachingProfile != null) {
            if (this.usernameUUIDLayer.save(cachingProfile)) { // Save the Username <--> UUID conversion for this player
                preCaching.call(cachingProfile);
                CacheResult<T> cacheResult = this.cache(cachingProfile, false);
                cached.call(cacheResult);
                return cacheResult;
            } else {
                // Fail; don't allow join
                return this.failCacheResult(cachingProfile, null, uniqueId, username, CacheSource.USERNAME_UUID, false);
            }
        } else {
            // Failed to pre-cache their profile.  Without this, we can't perform async caching or error/failure handling.
            // Since we won't be able to re-attempt to cache them while they are online, we deny the join.
            return new CacheResult<>(null, null, null, uniqueId, username, false,
                    CacheStage.FAILED, CacheSource.PRE_CACHING, false);
        }
    }

    public CacheResult<T> cache(CachingProfile<T> cachingProfile, boolean fromError) {
        cachingProfile.setStage(CacheStage.LOADING);
        if (!fromError && this.localLayer.has(cachingProfile.getUniqueId())) {
            T profile = this.localLayer.provide(cachingProfile);
            debugger.debug("Loaded profile for " + cachingProfile.getName() + " from " + cachingProfile.getLoadingSource().toString() + " layer");
            // Cached locally!  Return their local profile.
            return new CacheResult<>(cachingProfile, profile, null, cachingProfile.getUniqueId(),
                    cachingProfile.getName(), true, CacheStage.LOADED, cachingProfile.getLoadingSource(), true);
        } else {
            // Redis --> Mongo --> !mongo? --> create profile; save everywhere
            T redisProfile = this.redisLayer.provide(cachingProfile);
            if (redisProfile != null) {
                if (this.saveProfileAfterLoadCache(redisProfile, CacheSource.REDIS)) {
                    debugger.debug("Loaded profile for " + cachingProfile.getName() + " from " + cachingProfile.getLoadingSource().toString() + " layer");
                    // Success!  --> Saved to Local Cache and loaded from Redis
                    return new CacheResult<>(cachingProfile, redisProfile, null, cachingProfile.getUniqueId(),
                            cachingProfile.getName(), true, CacheStage.LOADED, cachingProfile.getLoadingSource(), true);
                }
                else{
                    debugger.debug("Could not save " + cachingProfile.getName() + "'s profile while trying to save after provision from Redis");
                    // Was loaded but could not be saved to the cache... ERROR; should not happen
                    return this.failCacheResult(cachingProfile, redisProfile, cachingProfile.getUniqueId(),
                            cachingProfile.getName(), cachingProfile.getLoadingSource(), true); // Still allow join to allow for error handling
                }
            } else {
                T mongoProfile = this.mongoLayer.provide(cachingProfile);
                if (mongoProfile != null) {
                    if (this.saveProfileAfterLoadCache(mongoProfile, CacheSource.MONGO)) {
                        debugger.debug("Loaded profile for " + cachingProfile.getName() + " from " + cachingProfile.getLoadingSource().toString() + " layer");
                        // Success!  --> Saved to Local Cache and loaded from MongoDB
                        return new CacheResult<>(cachingProfile, mongoProfile, null, cachingProfile.getUniqueId(),
                                cachingProfile.getName(), true, CacheStage.LOADED, cachingProfile.getLoadingSource(), true);
                    }
                    else {
                        debugger.debug("Could not save " + cachingProfile.getName() + "'s profile while trying to save after provision from MongoDB");
                        // Was loaded but could not be saved to the cache... ERROR; should not happen
                        return this.failCacheResult(cachingProfile, mongoProfile, cachingProfile.getUniqueId(),
                                cachingProfile.getName(), cachingProfile.getLoadingSource(), true); // Still allow join to allow for error handling
                    }
                } else {
                    if (!mongoLayer.getDatabase().isMongoConnected()) {
                        // Mongo is not connected.  Don't create a new profile; loading failure is likely a result of
                        // the database being down, and so we don't want to create a new profile and overwrite the data
                        debugger.debug("Not creating new profile for " + cachingProfile.getName() + " because MongoDB is offline");
                        return this.failCacheResult(cachingProfile, null, cachingProfile.getUniqueId(),
                                cachingProfile.getName(), cachingProfile.getLoadingSource(), true); // Still allow join to allow for error handling
                    }
                    if (fromError) {
                        debugger.debug("Not creating new profile for " + cachingProfile.getName() + " because calling from error handler");
                        // We don't want to create a profile for them during database outage // error handling
                        return this.failCacheResult(cachingProfile, null, cachingProfile.getUniqueId(),
                                cachingProfile.getName(), cachingProfile.getLoadingSource(), true); // Still allow join to allow for error handling
                    }
                    debugger.debug("Trying to load profile for " + cachingProfile.getName() + " from [new profile creation]");
                    // Player has no profile stored anywhere.  New user -->
                    T newProfile = creationLayer.provide(cachingProfile);
                    // Save
                    if (this.saveEverywhere(newProfile)) {
                        debugger.debug("Created & saved new profile for " + cachingProfile.getName());
                        // Success!  Saved new profile everywhere
                        return new CacheResult<>(cachingProfile, newProfile, null, cachingProfile.getUniqueId(),
                                cachingProfile.getName(), true, CacheStage.LOADED, CacheSource.NEW_PROFILE, true);
                    }
                    else {
                        // Failed to create the new profile; allow join and add to error handler
                        // Error handler should continue to try and create the profile
                        return this.failCacheResult(cachingProfile, newProfile, cachingProfile.getUniqueId(),
                                cachingProfile.getName(), cachingProfile.getLoadingSource(), true); // Still allow join to allow for error handling
                    }
                }
            }
        }
    }

    public boolean saveEverywhere(T profile) {
        boolean local = this.localLayer.save(profile);
        boolean redis = this.redisLayer.save(profile);
        boolean mongo = this.mongoLayer.save(profile);
        // Error handling is within the cache layers
        return local && redis && mongo;
    }

    /**
     * Initialize a profile after the player has joined
     * @param player Player
     * @param profile Profile
     */
    public void initProfile(Player player, T profile) {
        profile.setTemporary(false);
        profile.setHalted(false);
        profile.initialize(player);
        debugger.debug("Initialized profile for player " + player.getName());
    }

    private CacheResult<T> failCacheResult(CachingProfile<T> cachingProfile, T profile, String uniqueId, String username,
                                        CacheSource cacheSource, boolean allowJoin) {
        FailedCachedProfile<T> failedCachedProfile = null;
        if (allowJoin) {
            // Create them a failedCachedProfile and start the failure handling process
            if (failureHandler.hasFailedProfile(uniqueId)) {
                failedCachedProfile = failureHandler.getFailedProfile(uniqueId);
            } else {
                failedCachedProfile = failureHandler.startFailureHandling(cachingProfile);
            }
        }
        debugger.debug("Caching failed for player " + cachingProfile.getName() + " during " + cacheSource.toString());
        return new CacheResult<>(cachingProfile, profile, failedCachedProfile, uniqueId, username,
                false, CacheStage.FAILED, cacheSource, allowJoin);
    }

    public void saveAll(PayloadCallback<Map.Entry<Integer, Integer>> callback) {
        Payload.runASync(plugin, () -> {
            int count = 0;
            int failed = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                T profile = getLocalProfile(player);
                if (profile != null) {
                    try {
                        saveEverywhere(profile);
                        count++;
                    } catch (Exception ex) {
                        failed++;
                        player.sendMessage(ChatColor.RED + "Attempted to save all profiles but your player experienced an error caching.");
                        player.sendMessage(ChatColor.RED + "This should not happen.  Notify an administrator.");
                        player.sendMessage(ChatColor.RED + "If this problem persists and/or no administrators are available, please re-log.");
                        player.sendMessage(ChatColor.RED + "Sorry for the inconvenience.  This is to ensure that you do not lose any data.");
                        debugger.error(ex, "Exception while saving all for player " + player.getName());
                    }
                } else {
                    failed++;
                    player.sendMessage(ChatColor.RED + "Attempted to save all profiles but your player is not cached.");
                    player.sendMessage(ChatColor.RED + "This should not happen.  Notify an administrator.");
                    player.sendMessage(ChatColor.RED + "If this problem persists and/or no administrators are available, please re-log.");
                    player.sendMessage(ChatColor.RED + "Sorry for the inconvenience.  This is to ensure that you do not lose any data.");
                    debugger.debug("&c" + player.getName() + "'s player was not cached during a saveAll profiles call");
                }
            }
            callback.call(new AbstractMap.SimpleEntry<>(count, failed));
        });
    }

}
