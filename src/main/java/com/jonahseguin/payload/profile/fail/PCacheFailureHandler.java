package com.jonahseguin.payload.profile.fail;

import com.jonahseguin.payload.Payload;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.layers.ProfileCacheLayer;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.FailedCachedProfile;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.profile.ProfilePassable;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.profile.type.PCacheStage;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.plugin.Plugin;

@Getter
public class PCacheFailureHandler<X extends PayloadProfile> {

    private final Plugin plugin;
    private final PayloadProfileCache<X> cache;
    private final PCacheFailureTask<X> cacheFailureTask;
    private final Set<FailedCachedProfile<X>> failedCaches = new HashSet<>();

    public PCacheFailureHandler(PayloadProfileCache<X> cache, Plugin plugin) {
        this.plugin = plugin;
        this.cache = cache;
        this.cacheFailureTask = new PCacheFailureTask<>(this, cache);
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
            FailedCachedProfile<X> failedCachedProfile = new FailedCachedProfile<>(cachingProfile, cachingProfile.getName(), cachingProfile.getUniqueId(), cachingProfile.getLoginIp());
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

    public <T extends ProfilePassable, P extends ProfilePassable> X providerException(ProfileCacheLayer<X, T, P> layer, CachingProfile<X> target, Exception exception) {
        // The handler encountered an exception
        layer.error(exception, "ProfileCache Layer: " + layer.source().toString() +
                " errored while trying to provide for obj " + target.getName());
        return providerFailure(layer, target);
    }

    public <T extends ProfilePassable, P extends ProfilePassable> X providerFailure(ProfileCacheLayer<X, T, P> layer, CachingProfile<X> target) {
        target.setStage(PCacheStage.FAILED);
        // The handler could not provide the profile
        PCacheSource nextSource = layer.source().next();
        if (nextSource != null) {
            if (nextSource == PCacheSource.LOCAL) {
                layer.debug("[ProfileCache fail] Attempting to provide for " + target.getName() + " from Local layer");
                return cache.getLayerController().getLocalLayer().provide(target);
            } else if (nextSource == PCacheSource.REDIS) {
                layer.debug("[ProfileCache fail] Attempting to provide for " + target.getName() + " from Redis layer");
                return cache.getLayerController().getRedisLayer().provide(target);
            } else if (nextSource == PCacheSource.MONGO) {
                layer.debug("[ProfileCache fail] Attempting to provide for " + target.getName() + " from Mongo layer");
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

    private <T extends ProfilePassable, P extends ProfilePassable> void handleFailure(ProfileCacheLayer<X, T, P> layer, CachingProfile<X> target) {
        this.startFailureHandling(target);
        target.setStage(PCacheStage.FAILED);
    }

}
