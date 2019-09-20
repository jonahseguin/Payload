package com.jonahseguin.payload.base.task;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import org.bukkit.scheduler.BukkitTask;

public class PayloadAutoSaveTask<K, X extends Payload, D extends PayloadData> implements Runnable {

    private final PayloadCache<K, X, D> cache;

    private BukkitTask task = null;

    public PayloadAutoSaveTask(PayloadCache<K, X, D> cache) {
        this.cache = cache;
    }

    @Override
    public void run() {
        int failures = cache.saveAll();
        if (failures > 0) {
            cache.getErrorHandler().error(cache, failures + " Payload objects failed to save during auto-save.");
        } else {
            cache.getErrorHandler().debug(cache, "Auto-save completed successfully with 0 failures.");
        }
    }

    public boolean isRunning() {
        return this.task != null;
    }

    public void start() {
        if (!this.isRunning()) {
            this.task = cache.getPlugin().getServer().getScheduler().runTaskTimerAsynchronously(cache.getPlugin(), this, (cache.getSettings().getAutoSaveIntervalSeconds() * 20), (cache.getSettings().getAutoSaveIntervalSeconds() * 20));
        }
    }

    public void stop() {
        if (this.isRunning()) {
            this.task.cancel();
            this.task = null;
        }
    }

}
