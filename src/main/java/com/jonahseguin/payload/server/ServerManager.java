/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.database.PayloadDatabase;
import lombok.Getter;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ServerManager implements Runnable {

    public static final long ASSUME_OFFLINE_SECONDS = 60;
    public static final long PING_FREQUENCY_SECONDS = 10;

    private final PayloadDatabase database;
    private final PayloadServer thisServer;
    private final ConcurrentMap<String, PayloadServer> servers = new ConcurrentHashMap<>();
    private Jedis jedisSubscriber = null;
    private ServerPublisher publisher = null;
    private ServerSubscriber subscriber = null;

    public ServerManager(PayloadDatabase database) {
        this.database = database;
        this.thisServer = new PayloadServer(PayloadAPI.get().getPayloadID(), System.currentTimeMillis(), true);
        this.servers.put(this.thisServer.getName().toLowerCase(), this.thisServer);
    }

    public void startup() {
        this.publisher = new ServerPublisher(this);

        PayloadPlugin.runASync(PayloadPlugin.get(), () -> {
            this.jedisSubscriber = this.database.getResource();
            this.subscriber = new ServerSubscriber(this);
            this.jedisSubscriber.subscribe(this.subscriber,
                    "server-join", "server-ping", "server-quit", "server-update-name");
        });

        this.publisher.publishJoin();
        PayloadPlugin.get().getServer().getScheduler().runTaskTimerAsynchronously(PayloadPlugin.get(), this, (PING_FREQUENCY_SECONDS * 20), (PING_FREQUENCY_SECONDS * 20));
    }

    public void registerServer(String name, boolean online) {
        PayloadServer server = new PayloadServer(name, System.currentTimeMillis(), online);
        this.servers.put(name.toLowerCase(), server);
    }

    public boolean hasServer(String name) {
        return this.servers.containsKey(name.toLowerCase());
    }

    public PayloadServer getServer(String name) {
        return this.servers.get(name.toLowerCase());
    }

    public void handlePing(String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setLastPing(System.currentTimeMillis());
        } else {
            this.handleJoin(serverName);
        }
    }

    public void handleJoin(String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setOnline(true);
        } else {
            this.registerServer(serverName, true);
        }
    }

    public void handleQuit(String serverName) {
        if (this.servers.containsKey(serverName.toLowerCase())) {
            this.servers.get(serverName.toLowerCase()).setOnline(false);
        }
    }

    public void handleUpdateName(String oldName, String serverName) {
        if (this.servers.containsKey(oldName.toLowerCase())) {
            PayloadServer server = this.servers.get(oldName.toLowerCase());
            server.setName(serverName);
            this.servers.remove(oldName.toLowerCase());
            this.servers.put(serverName.toLowerCase(), server);
        }
    }

    public void doPing() {
        this.publisher.publishPing();
    }

    public void shutdown() {
        this.publisher.publishQuit();

        if (this.subscriber != null) {
            if (this.subscriber.isSubscribed()) {
                this.subscriber.unsubscribe();
            }
            this.subscriber = null;
        }
        this.publisher = null;
        if (this.jedisSubscriber != null) {
            this.jedisSubscriber.close();
        }
    }

    @Override
    public void run() {
        this.doPing();
        this.thisServer.setLastPing(System.currentTimeMillis());
        this.thisServer.setOnline(true);
        this.servers.forEach((name, server) -> {
            if (!name.equalsIgnoreCase(this.thisServer.getName())) {
                long pingExpiredAt = System.currentTimeMillis() - (ServerManager.ASSUME_OFFLINE_SECONDS * 1000);
                if (server.getLastPing() <= pingExpiredAt) {
                    // Assume they're offline
                    server.setOnline(false);
                }
            }
        });
    }
}
