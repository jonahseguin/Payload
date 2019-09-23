package com.jonahseguin.payload.server;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.database.PayloadDatabase;
import lombok.Getter;
import redis.clients.jedis.Jedis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ServerManager {

    private final PayloadDatabase database;
    private final PayloadServer thisServer;
    private final ConcurrentMap<String, PayloadServer> servers = new ConcurrentHashMap<>();
    private Jedis jedisSubscriber = null;
    private Jedis jedisPublisher = null;
    private ServerPublisher publisher = null;
    private ServerSubscriber subscriber = null;

    public ServerManager(PayloadDatabase database) {
        this.database = database;
        this.thisServer = new PayloadServer(PayloadAPI.get().getPayloadID(), System.currentTimeMillis(), true);
    }

    public void startup() {
        this.jedisPublisher = this.database.getResource();
        this.publisher = new ServerPublisher(this);

        PayloadPlugin.runASync(PayloadPlugin.get(), () -> {
            this.jedisSubscriber = this.database.getResource();
            this.subscriber = new ServerSubscriber(this);
            this.jedisSubscriber.subscribe(this.subscriber,
                    "server-join", "server-ping", "server-quit", "server-update-name");
        });
    }

    public void shutdown() {
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
        if (this.jedisPublisher != null) {
            this.jedisPublisher.close();
        }
    }

}
