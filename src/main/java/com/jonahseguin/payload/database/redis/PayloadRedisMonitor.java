/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database.redis;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.database.DatabaseDependent;
import com.jonahseguin.payload.database.DatabaseService;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;

public class PayloadRedisMonitor implements Runnable {

    private final PayloadPlugin payloadPlugin;
    private final DatabaseService database;
    private BukkitTask task = null;

    public PayloadRedisMonitor(PayloadPlugin payloadPlugin, DatabaseService database) {
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
        if (database.getJedisPool() == null) return;
        try {
            Jedis jedis = database.getMonitorJedis();
            if (jedis != null) {
                if (jedis.isConnected()) {
                    jedis.ping();
                    // Connected
                    this.handleConnected();
                } else {
                    // Disconnected
                    this.handleDisconnected();
                }
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
            if (this.database.getState().isRedisInitConnect()) {
                this.database.getHooks().forEach(DatabaseDependent::onRedisReconnect);
            } else {
                this.database.getState().setRedisInitConnect(true);
                this.database.getHooks().forEach(DatabaseDependent::onRedisInitConnect);
            }
            database.getErrorService().debug("Redis connected");
        }
    }

    private void handleDisconnected() {
        if (this.database.getState().isRedisConnected()) {
            this.database.getState().setRedisConnected(false);
            this.database.getHooks().forEach(DatabaseDependent::onRedisDisconnect);
            database.getErrorService().debug("Redis connection lost");
        }
        this.database.connectRedis();
    }

}
