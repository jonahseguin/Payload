package com.jonahseguin.payload.database;

import com.jonahseguin.payload.PayloadPlugin;
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
                    .runTaskTimerAsynchronously(PayloadPlugin.get(), this, 0L, 5L);
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
            }
        }
        catch (Exception ex) {
            // Failed, assume disconnected
            database.getErrorHandler().exception("Database: " + database.getName(), ex, "Error while monitoring Redis connection");
            this.handleDisconnected();
        }
    }

    private void handleConnected() {
        if (!this.database.getState().isRedisConnected()) {
            this.database.getState().setRedisConnected(true);
            database.getErrorHandler().debug("Database: " + database.getName(), "Redis connected");
        }
    }

    private void handleDisconnected() {
        if (this.database.getState().isRedisConnected()) {
            this.database.getState().setRedisConnected(false);
            database.getErrorHandler().debug("Database: " + database.getName(), "Redis connection lost");
        }
    }

}
