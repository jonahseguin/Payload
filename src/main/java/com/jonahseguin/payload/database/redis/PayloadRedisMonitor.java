package com.jonahseguin.payload.database.redis;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.database.PayloadDatabase;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;

public class PayloadRedisMonitor implements Runnable {

    private final PayloadDatabase database;
    private BukkitTask task = null;

    public PayloadRedisMonitor(PayloadDatabase database) {
        this.database = database;
    }

    public void start() {
        if (this.task == null) {
            this.task = PayloadPlugin.get().getServer().getScheduler()
                    .runTaskTimerAsynchronously(PayloadPlugin.get(), this, 60L, 60L);
        }
        else {
            throw new IllegalStateException("Redis Monitor is already running for database ID '" + database.getUuid() + "'; cannot start");
        }
    }

    public void stop() {
        if (this.task != null) {
            task.cancel();
        }
        else {
            throw new IllegalStateException("Redis Monitor is not running for database ID '" + database.getUuid() + "'; cannot stop");
        }
    }

    public boolean isRunning() {
        return this.task != null;
    }

    @Override
    public void run() {
        // Check if connection is alive
        Jedis jedis = database.getJedis();
        if (jedis == null) {
            // Jedis hasn't been initialized yet; ignore
            this.handleDisconnected();
            this.database.connectRedis();
            return;
        }
        try {
            if (jedis.isConnected()) {
                // Connected
                this.handleConnected();
            }
            else {
                // Disconnected
                this.handleDisconnected();

                this.database.connectRedis();
            }
        }
        catch (Exception ex) {
            // Failed, assume disconnected
            this.database.databaseError(ex, "Error in Redis Monitor task: " + ex.getMessage());
            if (PayloadPlugin.get().isDebug()) {
                ex.printStackTrace();
            }
            this.handleDisconnected();
        }
    }

    private void handleConnected() {
        if (!this.database.getState().isRedisConnected()) {
            this.database.getState().setRedisConnected(true);
            if (this.database.getState().isRedisInitConnect()) {
                this.database.getHooks().forEach(PayloadCache::onRedisReconnect);
            } else {
                this.database.getState().setRedisInitConnect(true);
                this.database.getHooks().forEach(PayloadCache::onRedisInitConnect);
            }
            database.databaseDebug("Redis connected");
        }
    }

    private void handleDisconnected() {
        if (this.database.getState().isRedisConnected()) {
            this.database.getState().setRedisConnected(false);
            this.database.getHooks().forEach(PayloadCache::onRedisDisconnect);
            database.databaseDebug("Redis connection lost");
        }
    }

}
