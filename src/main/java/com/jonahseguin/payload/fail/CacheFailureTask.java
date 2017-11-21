package com.jonahseguin.payload.fail;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.caching.CachingController;
import com.jonahseguin.payload.profile.FailedCachedProfile;
import com.jonahseguin.payload.profile.Profile;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

@Getter
public class CacheFailureTask<X extends Profile> implements Runnable {

    private final CacheFailureHandler<X> failureHandler;
    private final ProfileCache<X> profileCache;
    private BukkitTask bukkitTask = null;

    public CacheFailureTask(CacheFailureHandler<X> failureHandler, ProfileCache<X> profileCache) {
        this.failureHandler = failureHandler;
        this.profileCache = profileCache;
    }

    public void start() {
        if (this.bukkitTask == null) {
            this.bukkitTask = profileCache.getPlugin().getServer().getScheduler()
                    .runTaskTimerAsynchronously(profileCache.getPlugin(), this,
                            (profileCache.getSettings().getCacheFailRetryIntervalSeconds() * 20),
                            (profileCache.getSettings().getCacheFailRetryIntervalSeconds() * 20));
        }
    }

    public void stop() {
        if (this.bukkitTask != null) {
            this.bukkitTask.cancel();
        }
    }

    @Override
    public void run() {
        Set<FailedCachedProfile<X>> toRemove = new HashSet<>();
        for (FailedCachedProfile<X> profile : this.failureHandler.getFailedCaches()) {
            Player player = profile.tryToGetPlayer();
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.GRAY + "Attempting to load your profile...");
                CachingController<X> controller = this.profileCache.getController(player);
                X loadedProfile = controller.cache();
                controller.join(player);
                if (!controller.isJoinable()) {
                    toRemove.add(profile);
                    player.kickPlayer(ProfileCache.FAILED_CACHE_KICK_MESSAGE);
                    continue;
                }
                if (loadedProfile != null && !loadedProfile.isTemporary()) {
                    player.sendMessage(ChatColor.GREEN + "Success.  Profile loaded.");
                    toRemove.add(profile);
                    // Init their profile!
                    // Remove them from the failure handler
                } else {
                    player.sendMessage(ChatColor.RED + "Attempt failed.  We will continue to attempt to load your profile every 60 seconds...");
                }
            } else {
                // No need to bother trying to cache them anymore if they logged out...
                toRemove.add(profile);
            }
        }
        this.failureHandler.getFailedCaches().removeAll(toRemove);
    }
}
