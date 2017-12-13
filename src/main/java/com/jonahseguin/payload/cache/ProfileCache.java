package com.jonahseguin.payload.cache;

import com.jonahseguin.payload.Payload;
import com.jonahseguin.payload.caching.CachingController;
import com.jonahseguin.payload.caching.LayerExecutorHandler;
import com.jonahseguin.payload.event.ProfileCacheListener;
import com.jonahseguin.payload.event.ProfileHaltedListener;
import com.jonahseguin.payload.fail.CacheFailureHandler;
import com.jonahseguin.payload.layers.LayerController;
import com.jonahseguin.payload.profile.*;
import com.jonahseguin.payload.task.AfterJoinTask;
import com.jonahseguin.payload.task.CacheAutoSaveTask;
import com.jonahseguin.payload.task.CacheCleanupTask;
import com.jonahseguin.payload.task.JoinTaskRunnable;
import com.jonahseguin.payload.type.CacheSettings;
import com.jonahseguin.payload.util.PayloadCallback;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private final CacheCleanupTask<T> cleanupTask;
    private final AfterJoinTask afterJoinTask;
    private final LayerExecutorHandler<T> executorHandler;
    private CacheAutoSaveTask cacheAutoSaveTask;
    private boolean allowJoinsMode = false; // Internal join prevention; prevent joins when setting up/shutting down, etc.
    private ProfileCacheListener<T> profileCacheListener = null;
    private ProfileHaltedListener<T> profileHaltedListener = null;

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
        this.cleanupTask = new CacheCleanupTask<>(this);
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
            if (settings.isEnableHaltListener()) {
                profileHaltedListener = new ProfileHaltedListener<>(this);
                this.plugin.getServer().getPluginManager().registerEvents(profileHaltedListener, plugin);
            }
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

    public void addAfterJoinTask(CachingProfile<T> cachingProfile, JoinTaskRunnable runnable) {
        getAfterJoinTask().addTask(cachingProfile, runnable);
    }

    public T getProfile(Player player) {
        // getProfile --> UUID
        return getProfile(player.getUniqueId().toString());
    }

    public T getLocalProfile(Player player) {
        if (getLayerController().getLocalLayer().has(player.getUniqueId().toString())) {
            return this.getLayerController().getLocalLayer().get(player.getUniqueId().toString());
        }
        return null;
    }

    public T getProfile(String uniqueId) {
        // Ensure the profile we get is not temporary
        // If not cached, check for PreCached [CachingProfile] or FailedCachedProfile
        // --> could mean they are BEING cached, and not yet cached
        // --> so we have to return a TEMPORARY profile
        // --> and indicate somehow to the requesting method that the Profile we return is temporary
        if (getLayerController().getLocalLayer().has(uniqueId)) {
            return getLayerController().getLocalLayer().get(uniqueId);
        } else if (getLayerController().getPreCachingLayer().has(uniqueId)) {
            // Being cached currently; return their temporary profile.
            return getLayerController().getPreCachingLayer().get(uniqueId).getTemporaryProfile();
        } else if (getLayerController().getRedisLayer().has(uniqueId)) {
            return getLayerController().getRedisLayer().get(uniqueId);
        } else if (getLayerController().getMongoLayer().has(uniqueId)) {
            return getLayerController().getMongoLayer().get(uniqueId);
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

    public boolean saveEverywhere(T profile) {
        boolean local = this.getLayerController().getLocalLayer().save(profile);
        boolean redis = this.getLayerController().getRedisLayer().save(profile);
        boolean mongo = this.getLayerController().getMongoLayer().save(profile);
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
