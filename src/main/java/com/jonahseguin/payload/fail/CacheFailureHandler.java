package com.jonahseguin.payload.fail;

import com.jonahseguin.payload.Payload;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.layers.CacheLayer;
import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.FailedCachedProfile;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;
import com.jonahseguin.payload.type.CacheSource;
import com.jonahseguin.payload.type.CacheStage;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.plugin.Plugin;

@Getter
public class CacheFailureHandler<X extends Profile> {

    private final Plugin plugin;
    private final ProfileCache<X> cache;
    private final CacheFailureTask<X> cacheFailureTask;
    private final Set<FailedCachedProfile<X>> failedCaches = new HashSet<>();

    public CacheFailureHandler(ProfileCache<X> cache, Plugin plugin) {
        this.plugin = plugin;
        this.cache = cache;
        this.cacheFailureTask = new CacheFailureTask<>(this, cache);
        this.cacheFailureTask.start();
    }

    public boolean hasFailedProfile(String uniqueId) {
        return failedCaches.stream().anyMatch(failedCachedProfile -> failedCachedProfile.getUniqueId().equalsIgnoreCase(uniqueId));
    }

    public FailedCachedProfile<X> getFailedProfile(String uniqueId) {
        return failedCaches.stream().filter(failedCachedProfile -> failedCachedProfile.getUniqueId().equalsIgnoreCase(uniqueId)).findFirst().orElse(null);
    }

    public FailedCachedProfile<X> startFailureHandling(CachingProfile<X> cachingProfile) {
        if (!hasFailedProfile(cachingProfile.getUniqueId())) {
            FailedCachedProfile<X> failedCachedProfile = new FailedCachedProfile<>(cachingProfile, cachingProfile.getName(), cachingProfile.getUniqueId());
            this.failedCaches.add(failedCachedProfile);

            cache.getAfterJoinTask().addTask(cachingProfile, ((cp, player) -> {
                player.sendMessage(Payload.format("&4&lYour profile failed to load."));
                player.sendMessage(Payload.format("&cWe will continue to attempt to load your profile."));
                player.sendMessage(Payload.format("&cIf this problem persists, try re-logging and contact an administrator."));
                player.sendMessage(Payload.format("&7We apologize for the inconvenience.  Our database may be down, or we may be experiencing temporary issues."));
            }));
            return failedCachedProfile;
        } else {
            return getFailedProfile(cachingProfile.getUniqueId());
        }
    }

    public <T extends ProfilePassable, P extends ProfilePassable> X providerException(CacheLayer<X, T, P> layer, CachingProfile<X> target, Exception exception) {
        // The handler encountered an exception
        layer.error(exception, "Cache Layer: " + layer.source().toString() +
                " errored while trying to provide for player " + target.getName());
        return providerFailure(layer, target);
    }

    public <T extends ProfilePassable, P extends ProfilePassable> X providerFailure(CacheLayer<X, T, P> layer, CachingProfile<X> target) {
        target.setStage(CacheStage.FAILED);
        // The handler could not provide the profile
        CacheSource nextSource = layer.source().next();
        if (nextSource != null) {
            if (nextSource == CacheSource.LOCAL) {
                layer.debug("[Cache fail] Attempting to provide for " + target.getName() + " from local layer");
                return cache.getLayerController().getLocalLayer().provide(target);
            } else if (nextSource == CacheSource.REDIS) {
                layer.debug("[Cache fail] Attempting to provide for " + target.getName() + " from redis layer");
                return cache.getLayerController().getRedisLayer().provide(target);
            } else if (nextSource == CacheSource.MONGO) {
                layer.debug("[Cache fail] Attempting to provide for " + target.getName() + " from mongo layer");
                return cache.getLayerController().getMongoLayer().provide(target);
            } else {
                // Mongo failed; FAIL --> Add to failure handler task etc.
                handleFailure(layer, target);
            }
        } else {
            // Cannot provide; FAIL --> Add to failure handler task, etc.
            handleFailure(layer, target);
        }
        return null;
    }

    private <T extends ProfilePassable, P extends ProfilePassable> void handleFailure(CacheLayer<X, T, P> layer, CachingProfile<X> target) {
        this.startFailureHandling(target);
        target.setStage(CacheStage.FAILED);
    }

}
