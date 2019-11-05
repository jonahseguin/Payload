/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jonahseguin.payload.PayloadAPI;
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
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.ext.guice.GuiceExtension;
import dev.morphia.mapping.DefaultCreator;
import dev.morphia.mapping.MapperOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class PayloadDatabaseService implements DatabaseService {

    private final PayloadPlugin payloadPlugin;
    private final Plugin plugin;
    private final ServerService serverService;
    private final String name;
    private final Set<DatabaseDependent> hooks = new HashSet<>();
    private final DatabaseState state = new DatabaseState();
    private final ErrorService error;
    private final Morphia morphia;
    private boolean running = false;
    // MongoDB
    private PayloadMongo payloadMongo = null;
    private MongoClient mongoClient = null;
    private MongoDatabase database = null;
    private Datastore datastore = null;

    // Redis
    private PayloadRedis payloadRedis = null;
    private JedisPool jedisPool = null;
    private Jedis monitorJedis = null;
    private PayloadRedisMonitor redisMonitor = null;

    @Inject
    public PayloadDatabaseService(PayloadPlugin payloadPlugin, PayloadAPI api, Plugin plugin, Injector injector, ServerService serverService, @Database String name, @Database ErrorService error) {
        this.payloadPlugin = payloadPlugin;
        this.plugin = plugin;
        this.serverService = serverService;
        this.morphia = new Morphia();
        this.name = name;
        this.error = error;
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

        new GuiceExtension(morphia, injector);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public MongoClient getMongoClient() {
        return mongoClient;
    }

    @Override
    public Morphia getMorphia() {
        return morphia;
    }

    @Override
    public Datastore getDatastore() {
        return datastore;
    }

    @Override
    public Jedis getJedisResource() {
        return jedisPool.getResource();
    }

    @Override
    public JedisPool getJedisPool() {
        return jedisPool;
    }

    @Override
    public boolean isConnected() {
        return state.isDatabaseConnected();
    }

    @Override
    public boolean canFunction(@Nonnull DatabaseDependent dependent) {
        Preconditions.checkNotNull(dependent);
        return state.canCacheFunction(dependent);
    }

    @Override
    public ServerService getServerService() {
        return serverService;
    }

    @Override
    public String getName() {
        return database.getName();
    }

    @Override
    public Jedis getMonitorJedis() {
        return monitorJedis;
    }

    @Override
    public Set<DatabaseDependent> getHooks() {
        return hooks;
    }

    @Override
    public ErrorService getErrorService() {
        return error;
    }

    @Override
    public DatabaseState getState() {
        return state;
    }

    @Override
    public void hook(DatabaseDependent cache) {
        if (!this.hooks.contains(cache)) {
            this.hooks.add(cache);
        } else {
            throw new IllegalStateException("PayloadDatabase '" + name + "' has already hooked cache '" + cache + "'");
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
            error.capture(ex, "Failed Redis connection attempt");
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

    @Override
    public boolean start() {
        Preconditions.checkState(!running, "Database " + name + " is already running");
        boolean mongo = this.connectMongo();
        boolean redis = this.connectRedis();
        this.running = true;
        return mongo && redis;
    }

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
            this.datastore = this.morphia.createDatastore(mongoClient, payloadMongo.getDatabase());
            this.datastore.ensureIndexes();
            // If MongoDB fails it will automatically attempt to reconnect until it connects
            return true;
        } catch (Exception ex) {
            // Generic exception catch... just in case.
            return false; // Failed to connect.
        }
    }

    @Override
    public boolean shutdown() {
        Preconditions.checkState(running, "Database " + name + " is not running");
        if (this.redisMonitor != null) {
            this.redisMonitor.stop();
        }
        boolean mongo = this.disconnectMongo();
        boolean redis = this.disconnectRedis();
        this.running = false;
        return mongo && redis;
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
