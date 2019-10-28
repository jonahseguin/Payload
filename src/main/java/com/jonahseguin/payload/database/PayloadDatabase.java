/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.annotation.DatabaseName;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.database.mongo.PayloadMongo;
import com.jonahseguin.payload.database.mongo.PayloadMongoMonitor;
import com.jonahseguin.payload.database.redis.PayloadRedis;
import com.jonahseguin.payload.database.redis.PayloadRedisMonitor;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.ext.guice.GuiceExtension;
import dev.morphia.mapping.DefaultCreator;
import dev.morphia.mapping.MapperOptions;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The {@link PayloadDatabase} class provides the information required for connecting to the databases.
 * MongoDB and Redis information, which can be loaded from configurations or files.
 */
@Getter
@Setter
public class PayloadDatabase {

    private boolean started = false;

    private final PayloadAPI api;
    private final PayloadPlugin payloadPlugin;
    private final Plugin plugin;

    private final String name;
    private final String uuid = UUID.randomUUID().toString();
    private final Set<PayloadCache> hooks = new HashSet<>();
    private final DatabaseState state = new DatabaseState();

    private final PayloadMongo mongo;
    private final PayloadRedis redis;

    // MongoDB
    private MongoClient mongoClient = null;
    private MongoDatabase database = null;
    private final Morphia morphia;
    private Datastore datastore = null;

    // Redis
    private JedisPool jedisPool = null;
    private Jedis monitorJedis = null;
    private PayloadRedisMonitor redisMonitor = null;

    @Inject
    public PayloadDatabase(PayloadAPI api, PayloadPlugin payloadPlugin, Plugin plugin, @DatabaseName String name, PayloadMongo mongo, PayloadRedis redis) {
        this.payloadPlugin = payloadPlugin;
        this.plugin = plugin;
        this.name = name;
        this.mongo = mongo;
        this.redis = redis;
        this.api = api;
        this.morphia = new Morphia();
        api.registerDatabase(this);

        MapperOptions options = morphia.getMapper().getOptions();
        morphia.getMapper().setOptions(MapperOptions.builder(options)
                .objectFactory(new DefaultCreator(options) {
                    @Override
                    protected ClassLoader getClassLoaderForClass() {
                        return payloadPlugin.getPayloadClassLoader();
                    }
                })
                .build());
    }

    public void enableGuice(Injector injector) {
        new GuiceExtension(this.morphia, injector);
    }

    public void hookCache(PayloadCache cache) {
        if (!this.hooks.contains(cache)) {
            cache.setupDatabase(this);
            this.hooks.add(cache);
        }
        else {
            throw new IllegalStateException("PayloadDatabase '" + name + "' has already hooked cache '" + cache + "'");
        }
    }

    public boolean connectRedis() {
        try {
            this.state.setLastRedisConnectionAttempt(System.currentTimeMillis());
            // Try connection
            PayloadRedis payloadRedis = this.redis;

            if (this.jedisPool == null) {

                GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
                poolConfig.setMaxTotal(256);
                poolConfig.setMaxIdle(32);
                poolConfig.setMinIdle(2);

                if (payloadRedis.useURI()) {
                    jedisPool = new JedisPool(poolConfig, URI.create(payloadRedis.getUri()));
                } else {
                    if (payloadRedis.isAuth()) {
                        jedisPool = new JedisPool(poolConfig, payloadRedis.getAddress(), payloadRedis.getPort(), 2000, payloadRedis.getPassword(), payloadRedis.isSsl());
                    } else {
                        jedisPool = new JedisPool(poolConfig, payloadRedis.getAddress(), payloadRedis.getPort());
                    }
                }
            }

            if (this.monitorJedis == null) {
                this.monitorJedis = this.jedisPool.getResource();
                this.monitorJedis.ping();
            }

            return true; // No errors if we got here; success
        } catch (Exception ex) {
            this.databaseError(ex, "Failed Redis connection attempt");
            // Generic exception catch
            return false; // Failed to connect
        } finally {
            if (this.redisMonitor == null) {
                this.redisMonitor = new PayloadRedisMonitor(this);
            }
            if (!this.redisMonitor.isRunning()) {
                this.redisMonitor.start();
            }
        }
    }

    public Jedis getResource() {
        return this.jedisPool.getResource();
    }

    public boolean disconnectMongo() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
        return true; // MongoClient will handle the disconnecting and do it safely
    }

    public boolean disconnectRedis() {
        if (this.redisMonitor != null) {
            this.redisMonitor.stop();
        }
        this.jedisPool.close();
        return true;
    }

    public boolean start() {
        boolean mongo = this.connectMongo();
        boolean redis = this.connectRedis();
        this.started = true;
        return mongo && redis;
    }

    public boolean connectMongo() {
        if (this.mongoClient != null) {
            throw new IllegalStateException("MongoClient instance already exists for database " + this.name);
        }
        PayloadMongo payloadMongo = this.mongo; // MongoDB information

        try {
            MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder()
                    .addServerMonitorListener(new PayloadMongoMonitor(this));

            MongoClient mongoClient; // Client
            if (payloadMongo.useURI()) {
                // Using connection URI
                MongoClientURI uri = new MongoClientURI(payloadMongo.getUri(), optionsBuilder); // Pass options via builder (monitor)
                mongoClient = new MongoClient(uri);
            } else {
                // Using other info
                ServerAddress address = new ServerAddress(payloadMongo.getAddress(), payloadMongo.getPort()); // Our address
                if (payloadMongo.isAuth()) {
                    // Using auth
                    MongoCredential credential = MongoCredential.createCredential(payloadMongo.getUsername(),
                            payloadMongo.getAuthDatabase(), payloadMongo.getPassword().toCharArray());
                    mongoClient = new MongoClient(address, credential, optionsBuilder.build());
                } else {
                    // No auth
                    mongoClient = new MongoClient(address, optionsBuilder.build());
                }
            }
            this.mongoClient = mongoClient;
            this.datastore = this.morphia.createDatastore(mongoClient, payloadMongo.getDatabase());
            this.datastore.ensureIndexes();
            // If MongoDB fails it will automatically attempt to reconnect until it connects
            return true;
        } catch (Exception ex) {
            // Generic exception catch... just in case.
            return false; // Failed to connect.
        }
    }

    public boolean stop() {
        for (PayloadCache cache : this.getHooks()) {
            if (cache.isRunning()) {
                // Still running... don't just close the DB connection w/o proper shutdown
                this.databaseDebug("Database '" + this.name + "' stopped, stopping dependent cache: " + cache.getName());
                if (cache.stop()) {
                    this.databaseDebug("Stopped cache '" + cache.getName() + "' successfully.");
                } else {
                    this.databaseDebug("Failed to properly stop cache '" + cache.getName() + "', proceeding anyways.");
                }
            }
        }
        if (this.redisMonitor != null) {
            this.redisMonitor.stop();
        }
        boolean mongo = this.disconnectMongo();
        boolean redis = this.disconnectRedis();
        this.started = false;
        return mongo && redis;
    }

    public void databaseError(Throwable ex, String msg) {
        payloadPlugin.getLogger().severe("[Database: " + this.getName() + "] " + msg + " - " + ex.getMessage());
        //PayloadPlugin.get().alert(PayloadPermission.DEBUG, "&c[Payload][Database: " + this.getName() + "] " + msg + " - " + ex.getMessage());
        if (payloadPlugin.isDebug()) {
            ex.printStackTrace();
        }
    }

    public void databaseDebug(String msg) {
        payloadPlugin.alert(PayloadPermission.DEBUG, PLang.DEBUG_DATABASE, this.getName(), msg);
    }

}
