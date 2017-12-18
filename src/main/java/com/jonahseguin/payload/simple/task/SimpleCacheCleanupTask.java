package com.jonahseguin.payload.simple.task;

import com.jonahseguin.payload.simple.cache.PayloadSimpleCache;
import com.jonahseguin.payload.simple.simple.PlayerCacheable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;

public class SimpleCacheCleanupTask<X extends PlayerCacheable> implements Runnable {

    private final PayloadSimpleCache<X> cache;
    private BukkitTask task = null;

    public SimpleCacheCleanupTask(PayloadSimpleCache<X> cache) {
        this.cache = cache;
    }

    public void start() {
        if (task == null) {
            task = cache.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(cache.getPlugin(), this,
                    (cache.getSettings().getCacheCleanupCheckIntervalMinutes() * 60 * 20), (cache.getSettings().getCacheCleanupCheckIntervalMinutes() * 60 * 20));
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @Override
    public void run() {
        Iterator<String> it = cache.getExpiry().keySet().iterator();
        while (it.hasNext()) {
            String uuid = it.next();
            long expiry = cache.getExpiry().get(uuid);
            if (expiry <= System.currentTimeMillis()) {
                cache.removeFromCache(uuid);
                cache.getExpiry().remove(uuid);
            }
        }
    }
}
