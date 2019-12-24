/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database.redis;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.database.PayloadDatabase;
import org.bukkit.scheduler.BukkitTask;

public class PayloadRedisMonitor implements Runnable {

    private final PayloadPlugin payloadPlugin;
    private final PayloadDatabase database;
    private BukkitTask task = null;

    public PayloadRedisMonitor(PayloadPlugin payloadPlugin, PayloadDatabase database) {
        this.payloadPlugin = payloadPlugin;
        this.database = database;
    }

    public void start() {
        if (this.task == null) {
            this.task = payloadPlugin.getServer().getScheduler()
                    .runTaskTimerAsynchronously(payloadPlugin, this, 0L, 20L);
        }
        else {
            throw new IllegalStateException("Redis Monitor is already running for database ID '" + database.getName() + "'; cannot start");
        }
    }

    public void stop() {
        if (this.task != null) {
            task.cancel();
        }
        else {
            throw new IllegalStateException("Redis Monitor is not running for database ID '" + database.getName() + "'; cannot stop");
        }
    }

    public boolean isRunning() {
        return this.task != null;
    }

    @Override
    public void run() {
        // Check if connection is alive
        if (database.getRedis() == null) return;
        try {
            String reply = database.getRedis().sync().ping();

            if (reply.contains("PONG")) {
                this.handleConnected();
            } else {
                this.handleDisconnected();
                database.getErrorService().capture("Non-PONG ping reply in Redis Monitor: " + reply);
            }
        }
        catch (Exception ex) {
            // Failed, assume disconnected
            this.handleDisconnected();
            this.database.getErrorService().capture(ex, "Error in Redis Monitor task: " + ex.getMessage());
        }
    }

    private void handleConnected() {
        if (!this.database.getState().isRedisConnected()) {
            this.database.getState().setRedisConnected(true);
            if (!this.database.getState().isRedisInitConnect()) {
                this.database.getState().setRedisInitConnect(true);
                database.getErrorService().debug("Redis initial connection succeeded");
            } else {
                database.getErrorService().debug("Redis connection restored");
            }
        }
    }

    private void handleDisconnected() {
        if (this.database.getState().isRedisConnected()) {
            this.database.getState().setRedisConnected(false);
            database.getErrorService().capture("Redis connection lost");
        }
        this.database.connectRedis();
    }

}
