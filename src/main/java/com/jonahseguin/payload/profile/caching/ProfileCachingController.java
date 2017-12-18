package com.jonahseguin.payload.profile.caching;

import com.jonahseguin.payload.common.exception.CachingException;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.event.PayloadPlayerLoadedEvent;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.Profile;
import com.jonahseguin.payload.profile.profile.ProfilePassable;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.profile.type.PCacheStage;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Created by Jonah on 11/18/2017.
 * Project: Payload
 *
 * @ 3:58 PM
 */
@Getter
@Setter
public class ProfileCachingController<X extends Profile> {

    private final PayloadProfileCache<X> cache;
    private final ProfilePassable passable;
    private CachingProfile<X> cachingProfile;
    private X profile = null;
    private boolean joinable = true; // whether the obj could join (based on if they got at least a caching profile)
    private PCacheSource loadedFrom = null;
    private Player player = null;

    public ProfileCachingController(PayloadProfileCache<X> cache, ProfilePassable passable) {
        this.cache = cache;
        this.passable = passable;
    }

    public ProfileCachingController<X> withPlayer(Player player) {
        this.player = player;
        return this;
    }

    public X cache() {
        cachingProfile = tryPreCaching(); // Init Caching Profile
        if (cachingProfile != null) {
            cachingProfile.setController(this);
            cachingProfile.setStage(PCacheStage.INIT);
            cache.getLayerController().getUsernameUUIDLayer().provide(passable); // Save to Username <--> UUID
            cachingProfile.setStage(PCacheStage.LOADING);
            profile = tryLocal();
            loadedFrom = PCacheSource.LOCAL;
            if (profile == null) {
                profile = tryRedis();
                loadedFrom = PCacheSource.REDIS;
                if (profile == null) {
                    profile = tryMongo();
                    loadedFrom = PCacheSource.MONGO;
                    if (profile == null) {
                        profile = tryCreateNew();
                        loadedFrom = PCacheSource.NEW_PROFILE;
                    }
                }
            }
            if (profile != null) {
                if (saveProfileAfterLoadCache(profile, loadedFrom)) {
                    cachingProfile.setStage(PCacheStage.LOADED);
                    getCache().getPlugin().getServer().getPluginManager().callEvent(new PayloadPlayerLoadedEvent<>(cachingProfile, cache, profile));
                } else {
                    cache.getFailureHandler().startFailureHandling(cachingProfile);
                    return null;
                }
                return profile;
            } else {
                cache.getFailureHandler().startFailureHandling(cachingProfile);
                cachingProfile.setStage(PCacheStage.FAILED);
                cache.getDebugger().error(new CachingException("Could not provide a Profile for " + passable.getName()));
                return null;
            }
        } else {
            joinable = false;
            cache.getDebugger().error(new CachingException("Could not provide a caching profile for " + passable.getName()));
            return null;
        }
    }

    public void join(Player player) {
        this.player = player;
        if (!joinable) {
            player.kickPlayer(PayloadProfileCache.FAILED_CACHE_KICK_MESSAGE);
            return;
        }
        if (profile != null && !profile.isInitialized()) {
            cache.initProfile(player, profile);
        }
        if (cachingProfile != null) {
            if (profile != null && profile.isInitialized()) {
                cachingProfile.setStage(PCacheStage.DONE);
            }
            cachingProfile.setPlayer(player);
            if (cachingProfile.getStage() != PCacheStage.DONE) {
                // Is not DONE or LOADED; set up a temporary profile and continue to attempt to load their profile
                X profile = cachingProfile.getTemporaryProfile();
                if (profile != null) {
                    player.sendMessage(" ");
                    player.sendMessage(ChatColor.GRAY + "Your profile is being loaded.");
                    player.sendMessage(ChatColor.GRAY + "Please wait, your normal game-play will resume once your profile is loaded.");
                }
                else {
                    joinable = false;
                    player.kickPlayer(PayloadProfileCache.FAILED_CACHE_KICK_MESSAGE);
                }
            }
        }
    }

    private boolean saveProfileAfterLoadCache(X profile, PCacheSource except) {
        // Save profile to all layers /except/ the layer it was provided from
        boolean success = true;
        if (!except.equals(PCacheSource.LOCAL)) {
            boolean local = cache.getLayerController().getLocalLayer().save(profile);
            success = local;
            if (!local) {
                cache.getDebugger().debug("Local layer failed to save when saving Profile after load for obj " + profile.getName());
            }
        }
        if (!except.equals(PCacheSource.REDIS)) {
            boolean redis = cache.getLayerController().getRedisLayer().save(profile);
            if (success) {
                success = redis;
            }
            if (!redis) {
                cache.getDebugger().debug("Redis layer failed to save when saving Profile after load for obj " + profile.getName());
            }
        }
        if (!except.equals(PCacheSource.MONGO)) {
            boolean mongo = cache.getLayerController().getMongoLayer().save(profile);
            if (success) {
                success = mongo;
            }
            if (!mongo) {
                cache.getDebugger().debug("Mongo layer failed to save when saving Profile after load for obj " + profile.getName());
            }
        }
        return success;
    }

    private CachingProfile<X> tryPreCaching() {
        ProfileLayerResult<CachingProfile<X>> result = cache.getExecutorHandler().preCachingExecutor(passable).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        else {
            joinable = false;
        }
        if (result.isErrors()) {
            joinable = false;
        }
        return null;
    }

    private X tryLocal() {
        ProfileLayerResult<X> result = cache.getExecutorHandler().localExecutor(cachingProfile).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

    private X tryRedis() {
        ProfileLayerResult<X> result = cache.getExecutorHandler().redisExecutor(cachingProfile).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

    private X tryMongo() {
        ProfileLayerResult<X> result = cache.getExecutorHandler().mongoExecutor(cachingProfile).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

    private X tryCreateNew() {
        ProfileLayerResult<X> result = cache.getExecutorHandler().creationExecutor(passable).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

}