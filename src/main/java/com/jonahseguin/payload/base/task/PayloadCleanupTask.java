package com.jonahseguin.payload.base.task;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import org.bukkit.scheduler.BukkitTask;

public class PayloadCleanupTask<K, X extends Payload<K>, D extends PayloadData> implements Runnable {

    private final PayloadCache<K, X, D> cache;

    private BukkitTask task = null;

    public PayloadCleanupTask(PayloadCache<K, X, D> cache) {
        this.cache = cache;
    }

    @Override
    public void run() {
        cache.getLayerController().getLayers().forEach(layer -> {
            int cleaned = layer.cleanup();
            cache.getErrorHandler().debug(cache, "Cleaned " + cleaned + " payloads in layer: " + layer.layerName());
        });
    }

    public boolean isRunning() {
        return this.task != null;
    }

    public void start() {
        if (!this.isRunning()) {
            this.task = cache.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(cache.getPlugin(), this, (cache.getSettings().getCleanupIntervalSeconds() * 20), (cache.getSettings().getCleanupIntervalSeconds() * 20));
        }
    }

    public void stop() {
        if (this.isRunning()) {
            this.task.cancel();
            this.task = null;
        }
    }

}
