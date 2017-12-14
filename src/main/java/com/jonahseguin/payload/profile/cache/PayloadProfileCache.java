package com.jonahseguin.payload.profile.cache;

import com.jonahseguin.payload.Payload;
import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.profile.caching.ProfileCachingController;
import com.jonahseguin.payload.profile.caching.PLayerExecutorHandler;
import com.jonahseguin.payload.profile.event.ProfileCacheListener;
import com.jonahseguin.payload.profile.event.ProfileHaltedListener;
import com.jonahseguin.payload.profile.fail.PCacheFailureHandler;
import com.jonahseguin.payload.profile.layers.ProfileLayerController;
import com.jonahseguin.payload.profile.profile.*;
import com.jonahseguin.payload.profile.task.PAfterJoinTask;
import com.jonahseguin.payload.profile.task.PCacheAutoSaveTask;
import com.jonahseguin.payload.profile.task.PCacheCleanupTask;
import com.jonahseguin.payload.profile.task.PJoinTask;
import com.jonahseguin.payload.profile.type.ProfileCacheSettings;
import com.jonahseguin.payload.common.util.PayloadCallback;
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
public class PayloadProfileCache<T extends Profile> {

    private final String cacheId = UUID.randomUUID().toString();

    public static final String FAILED_CACHE_KICK_MESSAGE = ChatColor.RED + "An error occurred while loading your profile.\n" +
            ChatColor.RED + "This is should not happen.  Try re-logging and contact an administrator.\n" +
            ChatColor.GRAY + "We apologize for the inconvenience.";

    private final SimpleProfileCache<T> simpleCache = new SimpleProfileCache<>(this);
    private final ConcurrentMap<String, ProfileCachingController<T>> controllers = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final Class<T> profileClass;
    private final ProfileCacheSettings<T> settings;
    private final ProfileInstantiator<T> profileInstantiator;
    private final CacheDebugger debugger;
    private final CacheDatabase database;
    private final ProfileLayerController<T> layerController;
    private final PCacheFailureHandler<T> failureHandler;
    private final PCacheCleanupTask<T> cleanupTask;
    private final PAfterJoinTask afterJoinTask;
    private final PLayerExecutorHandler<T> executorHandler;
    private PCacheAutoSaveTask<T> cacheAutoSaveTask;
    private boolean allowJoinsMode = false; // Internal join prevention; prevent joins when setting up/shutting down, etc.
    private ProfileCacheListener<T> profileCacheListener = null;
    private ProfileHaltedListener<T> profileHaltedListener = null;

    public PayloadProfileCache(ProfileCacheSettings<T> settings, Class<T> clazz) {
        this.settings = settings;
        this.profileClass = clazz;
        this.plugin = settings.getPlugin();
        this.profileInstantiator = settings.getProfileInstantiator();
        this.debugger = settings.getDebugger();
        this.database = settings.getDatabase();
        this.failureHandler = new PCacheFailureHandler<>(this, settings.getPlugin());
        this.executorHandler = new PLayerExecutorHandler<>(this);
        this.layerController = new ProfileLayerController<>(this);
        this.cleanupTask = new PCacheCleanupTask<>(this);
        this.afterJoinTask = new PAfterJoinTask(this);
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
            this.cacheAutoSaveTask = new PCacheAutoSaveTask<>(this);
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

    public ProfileCachingController<T> getController(String username, String uniqueId) {
        if (this.controllers.containsKey(uniqueId)) {
            return this.controllers.get(uniqueId);
        }
        return new ProfileCachingController<>(this, new SimpleProfilePassable(uniqueId, username));
    }

    public ProfileCachingController<T> getController(Player player) {
        if (this.controllers.containsKey(player.getUniqueId().toString())) {
            return this.controllers.get(player.getUniqueId().toString());
        }
        return new ProfileCachingController<>(this, new SimpleProfilePassable(player.getUniqueId().toString(), player.getName()))
                .withPlayer(player);
    }

    public boolean hasController(String uniqueId) {
        return this.controllers.containsKey(uniqueId);
    }

    public void destroyController(String uniqueId) {
        this.controllers.remove(uniqueId);
    }

    public void addAfterJoinTask(CachingProfile<T> cachingProfile, PJoinTask runnable) {
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
     * Initialize a profile after the obj has joined
     * @param player Player
     * @param profile Profile
     */
    public void initProfile(Player player, T profile) {
        profile.setTemporary(false);
        profile.setHalted(false);
        profile.initialize(player);
        debugger.debug("Initialized profile for obj " + player.getName());
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
                        player.sendMessage(ChatColor.RED + "Attempted to save all profile but your obj experienced an error caching.");
                        player.sendMessage(ChatColor.RED + "This should not happen.  Notify an administrator.");
                        player.sendMessage(ChatColor.RED + "If this problem persists and/or no administrators are available, please re-log.");
                        player.sendMessage(ChatColor.RED + "Sorry for the inconvenience.  This is to ensure that you do not lose any data.");
                        debugger.error(ex, "Exception while saving all for obj " + player.getName());
                    }
                } else {
                    failed++;
                    player.sendMessage(ChatColor.RED + "Attempted to save all profile but your obj is not cached.");
                    player.sendMessage(ChatColor.RED + "This should not happen.  Notify an administrator.");
                    player.sendMessage(ChatColor.RED + "If this problem persists and/or no administrators are available, please re-log.");
                    player.sendMessage(ChatColor.RED + "Sorry for the inconvenience.  This is to ensure that you do not lose any data.");
                    debugger.debug("&c" + player.getName() + "'s obj was not cached during a saveAll profile call");
                }
            }
            callback.call(new AbstractMap.SimpleEntry<>(count, failed));
        });
    }

}
