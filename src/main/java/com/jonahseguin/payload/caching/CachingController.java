package com.jonahseguin.payload.caching;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.event.PayloadPlayerLoadedEvent;
import com.jonahseguin.payload.exception.CachingException;
import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;
import com.jonahseguin.payload.type.CacheSource;
import com.jonahseguin.payload.type.CacheStage;
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
public class CachingController<X extends Profile> {

    private final ProfileCache<X> cache;
    private final ProfilePassable passable;
    private CachingProfile<X> cachingProfile;
    private X profile = null;
    private boolean joinable = true; // whether the player could join (based on if they got at least a caching profile)
    private CacheSource loadedFrom = null;
    private Player player = null;

    public CachingController(ProfileCache<X> cache, ProfilePassable passable) {
        this.cache = cache;
        this.passable = passable;
    }

    public CachingController<X> withPlayer(Player player) {
        this.player = player;
        return this;
    }

    public X cache() {
        cachingProfile = tryPreCaching(); // Init Caching Profile
        if (cachingProfile != null) {
            cachingProfile.setController(this);
            cachingProfile.setStage(CacheStage.INIT);
            cache.getLayerController().getUsernameUUIDLayer().provide(passable); // Save to Username <--> UUID
            cachingProfile.setStage(CacheStage.LOADING);
            profile = tryLocal();
            loadedFrom = CacheSource.LOCAL;
            if (profile == null) {
                profile = tryRedis();
                loadedFrom = CacheSource.REDIS;
                if (profile == null) {
                    profile = tryMongo();
                    loadedFrom = CacheSource.MONGO;
                    if (profile == null) {
                        profile = tryCreateNew();
                        loadedFrom = CacheSource.NEW_PROFILE;
                    }
                }
            }
            if (profile != null) {
                if (saveProfileAfterLoadCache(profile, loadedFrom)) {
                    cachingProfile.setStage(CacheStage.LOADED);
                    getCache().getPlugin().getServer().getPluginManager().callEvent(new PayloadPlayerLoadedEvent<>(cache, profile));
                } else {
                    cache.getFailureHandler().startFailureHandling(cachingProfile);
                    return null;
                }
                return profile;
            } else {
                cache.getFailureHandler().startFailureHandling(cachingProfile);
                cachingProfile.setStage(CacheStage.FAILED);
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
            player.kickPlayer(ProfileCache.FAILED_CACHE_KICK_MESSAGE);
            return;
        }
        if (profile != null) {
            profile.initialize(player);
        }
        if (cachingProfile != null) {
            cachingProfile.setPlayer(player);
            if (cachingProfile.getStage() != CacheStage.DONE && cachingProfile.getStage() != CacheStage.LOADED) {
                // Is not DONE or LOADED; set up a temporary profile and continue to attempt to load their profile
                X profile = cachingProfile.getTemporaryProfile();
                if (profile != null) {
                    player.sendMessage(" ");
                    player.sendMessage(ChatColor.GRAY + "Your profile is being loaded.");
                    player.sendMessage(ChatColor.GRAY + "Please wait, your normal game-play will resume once your profile is loaded.");
                }
                else {
                    joinable = false;
                    player.kickPlayer(ProfileCache.FAILED_CACHE_KICK_MESSAGE);
                }
            }
        }
    }

    private boolean saveProfileAfterLoadCache(X profile, CacheSource except) {
        // Save profile to all layers /except/ the layer it was provided from
        boolean success = true;
        if (!except.equals(CacheSource.LOCAL)) {
            boolean local = cache.getLayerController().getLocalLayer().save(profile);
            success = local;
            if (!local) {
                cache.getDebugger().debug("Local layer failed to save when saving Profile after load for player " + profile.getName());
            }
        }
        if (!except.equals(CacheSource.REDIS)) {
            boolean redis = cache.getLayerController().getRedisLayer().save(profile);
            if (success) {
                success = redis;
            }
            if (!redis) {
                cache.getDebugger().debug("Redis layer failed to save when saving Profile after load for player " + profile.getName());
            }
        }
        if (!except.equals(CacheSource.MONGO)) {
            boolean mongo = cache.getLayerController().getMongoLayer().save(profile);
            if (success) {
                success = mongo;
            }
            if (!mongo) {
                cache.getDebugger().debug("Mongo layer failed to save when saving Profile after load for player " + profile.getName());
            }
        }
        return success;
    }

    private CachingProfile<X> tryPreCaching() {
        LayerResult<CachingProfile<X>> result = cache.getExecutorHandler().preCachingExecutor(passable).execute();
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
        LayerResult<X> result = cache.getExecutorHandler().localExecutor(cachingProfile).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

    private X tryRedis() {
        LayerResult<X> result = cache.getExecutorHandler().redisExecutor(cachingProfile).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

    private X tryMongo() {
        LayerResult<X> result = cache.getExecutorHandler().mongoExecutor(cachingProfile).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

    private X tryCreateNew() {
        LayerResult<X> result = cache.getExecutorHandler().creationExecutor(passable).execute();
        if (result.isSuccess()) {
            return result.getResult();
        }
        return null;
    }

}
