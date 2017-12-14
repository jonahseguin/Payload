package com.jonahseguin.payload.profile.task;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.Profile;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class PCacheCleanupTask<T extends Profile> implements Runnable {

    private final PayloadProfileCache<T> cache;
    private BukkitTask task;

    public PCacheCleanupTask(PayloadProfileCache<T> cache) {
        this.cache = cache;
        // Will cleanup cache every 30 minutes
        this.task = cache.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(cache.getPlugin(), this,
                (30 * 60 * 20), (30 * 60 * 20));
    }

    @Override
    public void run() {
        int usernameUUID = cache.getLayerController().getUsernameUUIDLayer().cleanup();
        int pre = cache.getLayerController().getPreCachingLayer().cleanup();
        int local = cache.getLayerController().getLocalLayer().cleanup();
        int redis = cache.getLayerController().getRedisLayer().cleanup();
        int mongo = cache.getLayerController().getMongoLayer().cleanup();

        int total = usernameUUID + pre + local + redis + mongo;

        cache.getDebugger().debug(ChatColor.GRAY + "Payload cleaned up " + total + " records across all layers.");
    }
}
