/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.exception.runtime.PayloadConfigException;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.database.mongo.PayloadMongo;
import com.jonahseguin.payload.database.mongo.PayloadMongoMonitor;
import com.jonahseguin.payload.database.redis.PayloadRedis;
import com.jonahseguin.payload.database.redis.PayloadRedisMonitor;
import com.jonahseguin.payload.server.ServerManager;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
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

    private final String name;
    private final String uuid = UUID.randomUUID().toString();
    private final Set<PayloadCache> hooks = new HashSet<>();
    private final DatabaseState state = new DatabaseState();
    private final ServerManager serverManager;

    private final PayloadMongo mongo;
    private final PayloadRedis redis;

    // MongoDB
    private MongoClient mongoClient = null;
    private MongoDatabase database = null;
    private Morphia morphia = null;
    private Datastore datastore = null;

    // Redis
    private JedisPool jedisPool = null;
    private Jedis monitorJedis = null;
    private PayloadRedisMonitor redisMonitor = null;

    public PayloadDatabase(String name, PayloadMongo mongo, PayloadRedis redis) {
        this.name = name;
        this.mongo = mongo;
        this.redis = redis;
        PayloadAPI.get().registerDatabase(this);
        this.serverManager = new ServerManager(this);
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

    /**
     * Same as {@link #loadConfigFile(File, String)}, but instead uses a File: loads that file as a YamlConfiguration
     * and then calls {@link #fromConfig(YamlConfiguration, String)}
     * The difference in this method from {@link #loadConfigFile(File, String)} is that it will
     * CREATE the file and copy the default config if it doesn't exist.
     *
     * @param file The file to load the config info from
     * @param name A unique name for the Database, for debugging purposes etc.
     * @return a new {@link PayloadDatabase} instance
     * @throws PayloadConfigException If any errors occur during loading the config or parsing database information
     */
    public static PayloadDatabase fromConfigFile(File file, String name) throws PayloadConfigException {
        if (!file.exists()) {
            try {
                file.createNewFile();
                File targetFile = new File(PayloadPlugin.get().getDataFolder() + File.separator + "database.yml");
                if (!targetFile.exists()) {
                    throw new PayloadConfigException("Default 'database.yml' file does not exist; could not be copied");
                }
                OutputStream os = new FileOutputStream(file);
                Files.copy(Paths.get(targetFile.toURI()), os);
                os.flush();
                os.close();
            } catch (IOException ex) {
                throw new PayloadConfigException("Error creating file '" + file.getName() + "' for new Payload config copy", ex);
            }
        }
        return loadConfigFile(file, name);
    }

    /**
     * Load an instance of a PayloadDatabase from a YAML configuration file (assumes certain key names!)
     * * Must follow the payload spec.
     *
     * @param config The YamlConfiguration to load the data from
     * @param name   A unique name for the Database, for debugging purposes etc.
     * @return a new {@link PayloadDatabase} instance
     * @throws PayloadConfigException Thrown if configuration format does not match Payload spec (as shown in default database.yml file)
     */
    public static PayloadDatabase fromConfig(YamlConfiguration config, String name) throws PayloadConfigException {
        ConfigurationSection mongoSection = config.getConfigurationSection("mongodb");
        ConfigurationSection redisSection = config.getConfigurationSection("redis");

        if (mongoSection != null) {
            if (redisSection != null) {
                PayloadMongo mongo = PayloadMongo.fromConfig(mongoSection);
                PayloadRedis redis = PayloadRedis.fromConfig(redisSection);

                return new PayloadDatabase(name, mongo, redis);
            } else {
                throw new PayloadConfigException("'redis' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
            }
        } else {
            throw new PayloadConfigException("'mongodb' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
        }
    }

    /**
     * Same as {@link #fromConfigFile(File, String)}, but uses the plugin's data folder and a file name
     * Will create and copy default if it doesn't exist
     * This is the recommended method to use for loading a database object from a config file
     *
     * @param plugin   Plugin
     * @param fileName File name (ending in .yml) to load from
     * @param name     A unique name for the Database, for debugging purposes etc.
     * @return a new {@link PayloadDatabase} instance
     * @throws PayloadConfigException If any errors occur during loading the config or parsing database information
     */
    public static PayloadDatabase fromConfigFile(Plugin plugin, String fileName, String name) throws PayloadConfigException {
        plugin.getDataFolder().mkdirs();
        return fromConfigFile(new File(plugin.getDataFolder() + File.separator + fileName), name);
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
                    mongoClient = new MongoClient(address, Collections.singletonList(credential), optionsBuilder.build());
                } else {
                    // No auth
                    mongoClient = new MongoClient(address, optionsBuilder.build());
                }
            }
            this.mongoClient = mongoClient;
            this.morphia = new Morphia();
            this.datastore = this.morphia.createDatastore(mongoClient, payloadMongo.getDatabase());

            // If MongoDB fails it will automatically attempt to reconnect until it connects
            return true;
        } catch (Exception ex) {
            // Generic exception catch... just in case.
            return false; // Failed to connect.
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
        this.serverManager.startup();
        this.started = true;
        return mongo && redis;
    }

    /**
     * Same as {@link #fromConfig(YamlConfiguration, String)}, but instead uses a File: loads that file as a YamlConfiguration
     * and then calls {@link #fromConfig(YamlConfiguration, String)}
     *
     * @param file The file to load the config info from
     * @param name A unique name for the Database, for debugging purposes etc.
     *
     * @return a new {@link PayloadDatabase} instance
     * @throws PayloadConfigException If any errors occur during loading the config or parsing database information
     */
    public static PayloadDatabase loadConfigFile(File file, String name) throws PayloadConfigException {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (FileNotFoundException ex) {
            throw new PayloadConfigException("Cannot load Payload Database info from a file that does not exist", ex);
        } catch (IOException ex) {
            throw new PayloadConfigException("Could not load Payload Database info from file", ex);
        } catch (InvalidConfigurationException ex) {
            throw new PayloadConfigException("Could not load Payload Database info from file (invalid config!)", ex);
        }
        return PayloadDatabase.fromConfig(config, name);
    }

    public boolean stop() {
        this.serverManager.shutdown();
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
        PayloadPlugin.get().getLogger().severe("[Database: " + this.getName() + "] " + msg + " - " + ex.getMessage());
        //PayloadPlugin.get().alert(PayloadPermission.DEBUG, "&c[Payload][Database: " + this.getName() + "] " + msg + " - " + ex.getMessage());
        if (PayloadPlugin.get().isDebug()) {
            ex.printStackTrace();
        }
    }

    public void databaseDebug(String msg) {
        PayloadPlugin.get().alert(PayloadPermission.DEBUG, PLang.DEBUG_DATABASE, this.getName(), msg);
    }

}
