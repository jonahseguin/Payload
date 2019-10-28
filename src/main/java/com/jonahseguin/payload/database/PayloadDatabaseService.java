/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.exception.runtime.PayloadConfigException;
import com.jonahseguin.payload.database.mongo.PayloadMongo;
import com.jonahseguin.payload.database.redis.PayloadRedis;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PayloadDatabaseService {

    private final PayloadAPI api;
    private final PayloadPlugin payloadPlugin;
    private final Plugin plugin;
    private final Injector injector;

    @Inject
    public PayloadDatabaseService(PayloadAPI api, PayloadPlugin payloadPlugin, Plugin plugin, Injector injector) {
        this.api = api;
        this.payloadPlugin = payloadPlugin;
        this.plugin = plugin;
        this.injector = injector;
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
    public PayloadDatabase fromConfig(YamlConfiguration config, String name) throws PayloadConfigException {
        ConfigurationSection mongoSection = config.getConfigurationSection("mongodb");
        ConfigurationSection redisSection = config.getConfigurationSection("redis");

        if (mongoSection != null) {
            if (redisSection != null) {
                PayloadMongo mongo = PayloadMongo.fromConfig(mongoSection);
                PayloadRedis redis = PayloadRedis.fromConfig(redisSection);

                PayloadDatabase database = new PayloadDatabase(api, payloadPlugin, plugin, name, mongo, redis);
                database.enableGuice(injector);
                return database;
            } else {
                throw new PayloadConfigException("'redis' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
            }
        } else {
            throw new PayloadConfigException("'mongodb' configuration section missing when loading PayloadDatabase fromConfig.  See example database.yml for proper formatting.");
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
    public PayloadDatabase fromConfigFile(File file, String name) throws PayloadConfigException {
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
        return loadConfigFile(file, name);
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
    public PayloadDatabase fromConfigFile(Plugin plugin, String fileName, String name) throws PayloadConfigException {
        plugin.getDataFolder().mkdirs();
        return fromConfigFile(new File(plugin.getDataFolder() + File.separator + fileName), name);
    }

    /**
     * Same as {@link #fromConfig(YamlConfiguration, String)}, but instead uses a File: loads that file as a YamlConfiguration
     * and then calls {@link #fromConfig(YamlConfiguration, String)}
     *
     * @param file The file to load the config info from
     * @param name A unique name for the Database, for debugging purposes etc.
     * @return a new {@link PayloadDatabase} instance
     * @throws PayloadConfigException If any errors occur during loading the config or parsing database information
     */
    public PayloadDatabase loadConfigFile(File file, String name) throws PayloadConfigException {
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
        return fromConfig(config, name);
    }

}
