/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.database.DatabaseService;
import lombok.Getter;
import org.bukkit.scheduler.BukkitTask;
import redis.clients.jedis.Jedis;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.*;

@Getter
public class PayloadServerService implements Runnable, ServerService {

    public static final long ASSUME_OFFLINE_SECONDS = 60;
    public static final long PING_FREQUENCY_SECONDS = 10;

    private final PayloadPlugin payloadPlugin;
    private final DatabaseService database;
    private final PayloadServer thisServer;
    private final ErrorService error;
    private final ConcurrentMap<String, PayloadServer> servers = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Jedis jedisSubscriber = null;
    private ServerPublisher publisher = null;
    private ServerSubscriber subscriber = null;
    private BukkitTask pingTask = null;
    private boolean running = false;

    @Inject
    public PayloadServerService(DatabaseService database, PayloadAPI api, PayloadPlugin payloadPlugin, @Database ErrorService error) {
        this.database = database;
        this.payloadPlugin = payloadPlugin;
        this.error = error;
        this.thisServer = new PayloadServer(api.getPayloadID(), System.currentTimeMillis(), true);
        this.servers.put(this.thisServer.getName().toLowerCase(), this.thisServer);
    }

    @Override
    public boolean start() {
        this.publisher = new ServerPublisher(this);

        this.executorService.submit(() -> {
            this.jedisSubscriber = this.database.getJedisResource();
            this.subscriber = new ServerSubscriber(this);
            this.jedisSubscriber.subscribe(this.subscriber,
                    "server-join", "server-ping", "server-quit", "server-update-name");
        });

        this.publisher.publishJoin();
        this.pingTask = payloadPlugin.getServer().getScheduler().runTaskTimerAsynchronously(payloadPlugin, this, (PING_FREQUENCY_SECONDS * 20), (PING_FREQUENCY_SECONDS * 20));
        running = true;
        return true;
    }

    @Override
    @Nonnull
    public PayloadServer register(@Nonnull String name, boolean online) {
        Preconditions.checkNotNull(name);
        PayloadServer server = new PayloadServer(name, System.currentTimeMillis(), online);
        this.servers.put(name.toLowerCase(), server);
        return server;
    }

    @Override
    public boolean has(@Nonnull String name) {
        Preconditions.checkNotNull(name);
        return this.servers.containsKey(name.toLowerCase());
    }

    @Override
    public Optional<PayloadServer> get(@Nonnull String name) {
        Preconditions.checkNotNull(name);
        return Optional.of(this.servers.get(name.toLowerCase()));
    }

    void handlePing(@Nonnull String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setLastPing(System.currentTimeMillis());
        } else {
            this.handleJoin(serverName);
        }
    }

    void handleJoin(@Nonnull String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setOnline(true);
        } else {
            this.register(serverName, true);
        }
    }

    void handleQuit(String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setOnline(false);
        }
    }

    void handleUpdateName(String oldName, String serverName) {
        if (this.servers.containsKey(oldName.toLowerCase())) {
            PayloadServer server = this.servers.get(oldName.toLowerCase());
            server.setName(serverName);
            this.servers.remove(oldName.toLowerCase());
            this.servers.put(serverName.toLowerCase(), server);
        }
    }

    private void doPing() {
        this.publisher.publishPing();
    }

    private void shutdownExecutor() {
        try {
            this.executorService.shutdown();
            this.executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            this.error.capture(ex, "Interrupted during shutdown of Server Manager's Executor Service");
        } finally {
            this.executorService.shutdownNow();
        }
    }

    @Override
    public boolean shutdown() {
        if (this.pingTask != null) {
            this.pingTask.cancel();
        }

        if (this.subscriber != null) {
            if (this.subscriber.isSubscribed()) {
                this.subscriber.unsubscribe();
            }
            this.subscriber = null;
        }

        this.publisher.publishQuit(); // Sync.
        this.shutdownExecutor();

        this.publisher = null;
        if (this.jedisSubscriber != null) {
            this.jedisSubscriber.close();
            this.jedisSubscriber = null;
        }
        running = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public Collection<PayloadServer> getServers() {
        return servers.values();
    }

    @Override
    public void run() {
        this.doPing();
        this.thisServer.setLastPing(System.currentTimeMillis());
        this.thisServer.setOnline(true);
        this.servers.forEach((name, server) -> {
            if (!name.equalsIgnoreCase(this.thisServer.getName())) {
                long pingExpiredAt = System.currentTimeMillis() - (PayloadServerService.ASSUME_OFFLINE_SECONDS * 1000);
                if (server.getLastPing() <= pingExpiredAt) {
                    // Assume they're offline
                    server.setOnline(false);
                }
            }
        });
    }
}
