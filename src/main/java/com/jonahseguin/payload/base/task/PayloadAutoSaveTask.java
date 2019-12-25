/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.task;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.type.Payload;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;

public class PayloadAutoSaveTask<K, X extends Payload<K>> implements Runnable {

    private final Cache<K, X> cache;

    private BukkitTask task = null;

    public PayloadAutoSaveTask(@Nonnull Cache<K, X> cache) {
        Preconditions.checkNotNull(cache);
        this.cache = cache;
    }

    @Override
    public void run() {
        int failures = cache.saveAll();
        if (failures > 0) {
            cache.getErrorService().capture(failures + " Payload objects failed to save during auto-save.");
        } else {
            cache.getErrorService().debug("Auto-save completed successfully with 0 failures.");
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
