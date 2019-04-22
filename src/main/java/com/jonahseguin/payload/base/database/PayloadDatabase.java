package com.jonahseguin.payload.base.database;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.exception.runtime.PayloadConfigException;
import com.jonahseguin.payload.base.type.Payload;
import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;

/**
 * The {@link PayloadDatabase} class provides the information required for connecting to the databases.
 * MongoDB and Redis information, which can be loaded from configurations or files.
 */
@Data
public class PayloadDatabase<K, X extends Payload> {

    private final PayloadCache<K, X> cache;

    private final PayloadMongo mongo;
    private final PayloadRedis redis;

    // MongoDB
    private MongoClient mongoClient = null;
    private MongoDatabase database = null;

    public boolean connectMongo() {
        PayloadMongo payloadMongo = this.mongo; // MongoDB information

        try {
            MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder()
                    .addServerMonitorListener(new PayloadMongoMonitor<>(this.cache));

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
                }
                else {
                    // No auth
                    mongoClient = new MongoClient(address, optionsBuilder.build());
                }
            }
            this.mongoClient = mongoClient;
            return true;
        }
        catch (Exception ex) {
            // Generic exception catch... just in case.
            return false; // Failed to connect.
        }
    }

    /**
     * Load an instance of a PayloadDatabase from a YAML configuration file (assumes certain key names!)
     *      * Must follow the payload spec.
     * @param config The YamlConfiguration to load the data from
     * @return a new {@link PayloadDatabase} instance
     * @throws PayloadConfigException Thrown if configuration format does not match Payload spec (as shown in default database.yml file)
     */
    public static <K, X extends Payload> PayloadDatabase<K, X> fromConfig(PayloadCache<K, X> cache, YamlConfiguration config) throws PayloadConfigException {
        ConfigurationSection mongoSection = config.getConfigurationSection("mongodb");
        ConfigurationSection redisSection = config.getConfigurationSection("redis");

        if (mongoSection != null) {
            if (redisSection != null) {
                PayloadMongo mongo = PayloadMongo.fromConfig(mongoSection);
                PayloadRedis redis = PayloadRedis.fromConfig(redisSection);

                return new PayloadDatabase<>(cache, mongo, redis);
            }
            else {
                throw new PayloadConfigException("'redis' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
            }
        }
        else {
            throw new PayloadConfigException("'mongo' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
        }
    }

    /**
     * Same as {@link #fromConfig(PayloadCache, YamlConfiguration)}, but instead uses a File: loads that file as a YamlConfiguration
     * and then calls {@link #fromConfig(PayloadCache, YamlConfiguration)}
     * @param file The file to load the config info from
     * @return a new {@link PayloadDatabase} instance
     * @throws PayloadConfigException If any errors occur during loading the config or parsing database information
     */
    public static <K, X extends Payload> PayloadDatabase<K, X> fromConfigFile(PayloadCache<K, X> cache, File file) throws PayloadConfigException {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        }
        catch (FileNotFoundException ex) {
            throw new PayloadConfigException("Cannot load Payload Database info from a file that does not exist", ex);
        }
        catch (IOException ex) {
            throw new PayloadConfigException("Could not load Payload Database info from file", ex);
        }
        catch (InvalidConfigurationException ex) {
            throw new PayloadConfigException("Could not load Payload Database info from file (invalid config!)", ex);
        }
        return PayloadDatabase.fromConfig(cache, config);
    }

}
