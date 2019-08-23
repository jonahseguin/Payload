package com.jonahseguin.payload.database;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.error.DefaultErrorHandler;
import com.jonahseguin.payload.base.error.PayloadErrorHandler;
import com.jonahseguin.payload.base.exception.runtime.PayloadConfigException;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import redis.clients.jedis.Jedis;

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
@Data
public class PayloadDatabase {

    private boolean started = false;

    private final String name;
    private final String uuid = UUID.randomUUID().toString();
    private final Set<PayloadCache> hooks = new HashSet<>();
    private PayloadErrorHandler errorHandler = new DefaultErrorHandler();
    private final DatabaseState state = new DatabaseState();

    private final PayloadMongo mongo;
    private final PayloadRedis redis;

    // MongoDB
    private MongoClient mongoClient = null;
    private MongoDatabase database = null;
    private Morphia morphia = null;
    private Datastore datastore = null;

    // Redis
    private Jedis jedis = null;
    private PayloadRedisMonitor redisMonitor = null;

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
                file.mkdirs();
                file.createNewFile();
                File targetFile = new File(PayloadPlugin.get().getDataFolder() + File.separator + "database.yml");
                if (!targetFile.exists()) {
                    throw new PayloadConfigException("Default 'database.yml' file does not exist; could not be copied");
                }
                OutputStream os = new FileOutputStream(targetFile);
                Files.copy(Paths.get(file.toURI()), os);
                os.close();
            } catch (IOException ex) {
                throw new PayloadConfigException("Error creating file '" + file.getName() + "' for new Payload config copy", ex);
            }
        }
        return loadConfigFile(file, name);
    }

    public boolean start() {
        boolean mongo = this.connectMongo();
        boolean redis = this.connectRedis();
        return mongo && redis;
    }

    public boolean stop() {
        for (PayloadCache cache : this.getHooks()) {
            if (cache.isRunning()) {
                // Still running... don't just close the DB connection w/o proper shutdown
                PayloadPlugin.get().getLogger().info("[Payload] Database '" + this.name + "' stopped, stopping dependent cache: " + cache.getName());
            }
        }
        boolean mongo = this.disconnectMongo();
        boolean redis = this.disconnectRedis();
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
        if (this.jedis != null) {
            throw new IllegalStateException("Jedis instance already exists for database " + this.name);
        }
        this.redisMonitor = new PayloadRedisMonitor(this);
        this.redisMonitor.start();
        try {
            // Try connection
            PayloadRedis payloadRedis = this.redis;
            if (payloadRedis.useURI()) {
                jedis = new Jedis(URI.create(payloadRedis.getUri()));
            } else {
                jedis = new Jedis(payloadRedis.getAddress(), payloadRedis.getPort());
            }
            jedis.connect();
            if (payloadRedis.isAuth()) {
                jedis.auth(payloadRedis.getPassword());
            }
            return true; // No errors if we got here; success
        } catch (Exception ex) {
            // Generic exception catch
            return false; // Failed to connect
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
        if (this.jedis == null) {
            // Was never initialized so we'll just stop here
            return true;
        }
        if (this.jedis.isConnected()) {
            this.jedis.disconnect();
        }
        this.jedis.close();
        return true;
    }

    /**
     * Load an instance of a PayloadDatabase from a YAML configuration file (assumes certain key names!)
     * * Must follow the payload spec.
     *
     * @param config The YamlConfiguration to load the data from
     * @param name A unique name for the Database, for debugging purposes etc.
     *
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
            throw new PayloadConfigException("'mongo' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
        }
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

    /**
     * Same as {@link #fromConfigFile(File, String)}, but uses the plugin's data folder and a file name
     * Will create and copy default if it doesn't exist
     * This is the recommended method to use for loading a database object from a config file
     * @param plugin Plugin
     * @param fileName File name (ending in .yml) to load from
     * @param name A unique name for the Database, for debugging purposes etc.
     * @return a new {@link PayloadDatabase} instance
     *
     * @throws PayloadConfigException  If any errors occur during loading the config or parsing database information
     */
    public static PayloadDatabase fromConfigFile(Plugin plugin, String fileName, String name) throws PayloadConfigException {
        return fromConfigFile(new File(plugin.getDataFolder() + File.separator + fileName), name);
    }

}
