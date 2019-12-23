/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.database.DatabaseService;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
@Singleton
public class PayloadServerService implements Runnable, ServerService {

    public static final long ASSUME_OFFLINE_SECONDS = 60;
    public static final long PING_FREQUENCY_SECONDS = 20;

    private final PayloadAPI api;
    private final String name;
    private final PayloadPlugin payloadPlugin;
    private final DatabaseService database;
    private final PayloadServer thisServer;
    private final ErrorService error;
    private final ConcurrentMap<String, PayloadServer> servers = new ConcurrentHashMap<>();
    private ServerPublisher publisher = null;
    private BukkitTask pingTask = null;
    private RedisPubSubReactiveCommands<String, String> reactive = null;
    private boolean running = false;

    @Inject
    public PayloadServerService(PayloadAPI api, DatabaseService database, PayloadPlugin payloadPlugin, @Database ErrorService error, @Database String name) {
        this.api = api;
        this.name = name;
        this.database = database;
        this.payloadPlugin = payloadPlugin;
        this.error = error;
        this.thisServer = new PayloadServer(payloadPlugin.getLocal().getPayloadID(), System.currentTimeMillis(), true);
        this.servers.put(this.thisServer.getName().toLowerCase(), this.thisServer);
    }

    @Override
    public boolean start() {
        try {
            this.publisher = new ServerPublisher(this);

            boolean sub = subscribe();

            this.publisher.publishJoin();
            this.pingTask = payloadPlugin.getServer().getScheduler().runTaskTimerAsynchronously(payloadPlugin, this, (PING_FREQUENCY_SECONDS * 20), (PING_FREQUENCY_SECONDS * 20));
            running = true;
            return sub;
        } catch (Exception ex) {
            error.capture(ex, "Error starting Server Service for database: " + name);
            return false;
        }
    }

    private boolean subscribe() {
        try {
            StatefulRedisPubSubConnection<String, String> connection = database.getRedisPubSub();
            reactive = connection.reactive();

            reactive.subscribe("payload-server-join", "payload-server-ping", "payload-server-quit", "payload-server-update-name").subscribe();

            reactive.observeChannels()
                    .filter(pm -> !pm.getMessage().equalsIgnoreCase(database.getServerService().getThisServer().getName()))
                    .filter(pm -> pm.getChannel().equalsIgnoreCase("payload-server-join") ||
                            pm.getChannel().equalsIgnoreCase("payload-server-ping") ||
                            pm.getChannel().equalsIgnoreCase("payload-server-quit") ||
                            pm.getChannel().equalsIgnoreCase("payload-server-update-name"))
                    .doOnNext(patternMessage -> {
                        ServerEvent event = ServerEvent.fromChannel(patternMessage.getChannel());
                        if (event != null) {
                            if (event.equals(ServerEvent.JOIN)) {
                                handleJoin(patternMessage.getMessage());
                            } else if (event.equals(ServerEvent.QUIT)) {
                                handleQuit(patternMessage.getMessage());
                            } else if (event.equals(ServerEvent.PING)) {
                                handlePing(patternMessage.getMessage());
                            } else if (event.equals(ServerEvent.UPDATE_NAME)) {
                                Document data = Document.parse(patternMessage.getMessage());
                                String oldName = data.getString("old");
                                String newName = data.getString("new");
                                handleUpdateName(oldName, newName);
                            }
                        }
                    }).subscribe();

            return true;
        } catch (Exception ex) {
            database.getErrorService().capture(ex, "Error subscribing in Payload Server Service");
            return false;
        }
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
        return Optional.ofNullable(this.servers.get(name.toLowerCase()));
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

    @Override
    public boolean shutdown() {
        try {
            if (this.pingTask != null) {
                this.pingTask.cancel();
            }
            if (reactive != null) {
                reactive.unsubscribe("payload-server-join", "payload-server-ping", "payload-server-quit", "payload-server-update-name");
            }

            this.publisher.publishQuit(); // Sync.
            this.publisher = null;
            running = false;
            return true;
        } catch (Exception ex) {
            error.capture("Error shutting down Server Service for database: " + name);
            return false;
        }
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
