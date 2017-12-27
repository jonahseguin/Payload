package com.jonahseguin.payload.profile.fail;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.caching.ProfileCachingController;
import com.jonahseguin.payload.profile.profile.FailedCachedProfile;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class PCacheFailureTask<X extends PayloadProfile> implements Runnable {

    private final PCacheFailureHandler<X> failureHandler;
    private final PayloadProfileCache<X> profileCache;
    private BukkitTask bukkitTask = null;

    public PCacheFailureTask(PCacheFailureHandler<X> failureHandler, PayloadProfileCache<X> profileCache) {
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
                ProfileCachingController<X> controller = this.profileCache.getController(player.getName(), player.getUniqueId().toString());

                X result = controller.cache();
                if(!controller.isJoinable()) {
                    player.kickPlayer(PayloadProfileCache.FAILED_CACHE_KICK_MESSAGE);
                    toRemove.add(profile);
                    continue;
                }
                if (result != null) {
                    player.sendMessage(ChatColor.GREEN + "Success.  PayloadProfile loaded.");
                    toRemove.add(profile);
                    controller.join(player);
                }
                else {
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
