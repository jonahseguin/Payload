/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.base.exception.runtime.PayloadConfigException;
import com.jonahseguin.payload.database.mongo.PayloadMongo;
import com.jonahseguin.payload.database.mongo.PayloadMongoMonitor;
import com.jonahseguin.payload.database.redis.PayloadRedis;
import com.jonahseguin.payload.database.redis.PayloadRedisMonitor;
import com.jonahseguin.payload.server.ServerService;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
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
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Singleton
public class InternalPayloadDatabase implements PayloadDatabase {

    private final DatabaseState state = new DatabaseState();
    private final String name;
    private final Plugin plugin;
    private final ErrorService errorService;
    private final PayloadPlugin payloadPlugin;
    private final Set<DatabaseDependent> hooks = new HashSet<>();
    private final Injector injector;

    private ServerService serverService = null;
    private boolean running = false;

    // MongoDB
    private PayloadMongo payloadMongo = null;
    private MongoClient mongoClient = null;
    private MongoDatabase database = null;

    // Redis
    private PayloadRedis payloadRedis = null;
    private JedisPool jedisPool = null;
    private Jedis monitorJedis = null;
    private PayloadRedisMonitor redisMonitor = null;

    @Inject
    public InternalPayloadDatabase(PayloadPlugin payloadPlugin, @Database ErrorService errorService, @Database String name, Plugin plugin, Injector injector) {
        this.payloadPlugin = payloadPlugin;
        this.errorService = errorService;
        this.name = name;
        this.plugin = plugin;
        this.injector = injector;
    }

    @Override
    public boolean start() {
        Preconditions.checkState(!running, "Database " + name + " is already running");
        errorService.debug("Starting database " + name);
        fromConfigFile("database.yml");
        boolean mongo = this.connectMongo();
        boolean redis = this.connectRedis();
        boolean server = true;
        if (serverService == null) {
            serverService = injector.getInstance(ServerService.class);
        }
        if (!serverService.isRunning()) {
            server = serverService.start();
        }
        running = true;
        if (!mongo) {
            payloadPlugin.getLogger().severe("Database " + name + ": Failed to start MongoDB");
        }
        if (!redis) {
            payloadPlugin.getLogger().severe("Database " + name + ": Failed to start Redis");
        }
        if (!server) {
            payloadPlugin.getLogger().severe("Database " + name + ": Failed to start Server Service");
        }
        errorService.debug("Started database " + name);
        return mongo && redis && server;
    }

    @Override
    public boolean shutdown() {
        Preconditions.checkState(running, "Database " + name + " is not running");
        boolean server = true;
        if (serverService != null) {
            server = serverService.shutdown();
        }
        if (this.redisMonitor != null) {
            this.redisMonitor.stop();
        }
        boolean mongo = this.disconnectMongo();
        boolean redis = this.disconnectRedis();
        this.running = false;
        if (!mongo) {
            payloadPlugin.getLogger().severe("Database " + name + ": Failed to shutdown MongoDB");
        }
        if (!redis) {
            payloadPlugin.getLogger().severe("Database " + name + ": Failed to shutdown Redis");
        }
        if (!server) {
            payloadPlugin.getLogger().severe("Database " + name + ": Failed to shutdown Server Service");
        }
        errorService.debug("Shutdown database " + name);
        return mongo && redis && server;
    }

    @Override
    public boolean connectMongo() {
        Preconditions.checkNotNull(payloadMongo, "Please load database " + name + " data from config before connecting");
        if (this.mongoClient != null) {
            throw new IllegalStateException("MongoClient instance already exists for database " + this.name);
        }

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
            // the datastore will be setup in the service class
            // If MongoDB fails it will automatically attempt to reconnect until it connects
            return true;
        } catch (Exception ex) {
            // Generic exception catch... just in case.
            return false; // Failed to connect.
        }
    }

    @Override
    public boolean connectRedis() {
        Preconditions.checkNotNull(payloadRedis, "Please load database " + name + " data from config before connecting");
        try {
            this.state.setLastRedisConnectionAttempt(System.currentTimeMillis());
            // Try connection

            if (this.jedisPool == null) {

                GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
                poolConfig.setMaxTotal(64);
                poolConfig.setMaxIdle(16);
                poolConfig.setMinIdle(8);

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
            errorService.capture(ex, "Failed Redis connection attempt");
            // Generic exception catch
            return false; // Failed to connect
        } finally {
            if (this.redisMonitor == null) {
                this.redisMonitor = new PayloadRedisMonitor(payloadPlugin, this);
            }
            if (!this.redisMonitor.isRunning()) {
                this.redisMonitor.start();
            }
        }
    }

    private boolean disconnectMongo() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
        return true; // MongoClient will handle the disconnecting and do it safely
    }

    private boolean disconnectRedis() {
        if (this.redisMonitor != null) {
            this.redisMonitor.stop();
        }
        this.jedisPool.close();
        return true;
    }

    /**
     * Load an instance of a PayloadDatabase from a YAML configuration file (assumes certain key names!)
     * * Must follow the payload spec.
     *
     * @param config The YamlConfiguration to load the data from
     * @throws PayloadConfigException Thrown if configuration format does not match Payload spec (as shown in default database.yml file)
     */
    public void fromConfig(YamlConfiguration config) throws PayloadConfigException {
        ConfigurationSection mongoSection = config.getConfigurationSection("mongodb");
        ConfigurationSection redisSection = config.getConfigurationSection("redis");

        if (mongoSection != null) {
            if (redisSection != null) {
                payloadMongo = PayloadMongo.fromConfig(mongoSection);
                payloadRedis = PayloadRedis.fromConfig(redisSection);
            } else {
                throw new PayloadConfigException("'redis' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
            }
        } else {
            throw new PayloadConfigException("'mongodb' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
        }
    }

    /**
     * Same as {@link #loadConfigFile(File)}, but instead uses a File: loads that file as a YamlConfiguration
     * and then calls {@link #fromConfig(YamlConfiguration)}
     * The difference in this method from {@link #loadConfigFile(File)} is that it will
     * CREATE the file and copy the default config if it doesn't exist.
     *
     * @param file The file to load the config info from
     * @throws PayloadConfigException If any errors occur during loading the config or parsing database information
     */
    public void fromConfigFile(File file) throws PayloadConfigException {
        if (!file.exists()) {
            try {
                file.createNewFile();
                File targetFile = new File(payloadPlugin.getDataFolder() + File.separator + "database.yml");
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
        loadConfigFile(file);
    }

    /**
     * Same as {@link #fromConfigFile(File)}, but uses the plugin's data folder and a file name
     * Will create and copy default if it doesn't exist
     * This is the recommended method to use for loading a database object from a config file
     *
     * @param fileName File name (ending in .yml) to load from
     * @throws PayloadConfigException If any errors occur during loading the config or parsing database information
     */
    public void fromConfigFile(String fileName) throws PayloadConfigException {
        plugin.getDataFolder().mkdirs();
        fromConfigFile(new File(plugin.getDataFolder() + File.separator + fileName));
    }

    /**
     * Same as {@link #fromConfig(YamlConfiguration)}, but instead uses a File: loads that file as a YamlConfiguration
     * and then calls {@link #fromConfig(YamlConfiguration)}
     *
     * @param file The file to load the config info from
     * @throws PayloadConfigException If any errors occur during loading the config or parsing database information
     */
    public void loadConfigFile(File file) throws PayloadConfigException {
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
        fromConfig(config);
    }

}
