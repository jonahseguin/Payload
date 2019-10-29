/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.annotation.DatabaseName;
import com.jonahseguin.payload.server.ServerManager;
import com.mongodb.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.JedisPool;

public class DatabaseModule extends AbstractModule {

    private final PayloadAPI api;
    private final PayloadPlugin payloadPlugin;
    private final Plugin plugin;

    public DatabaseModule(PayloadAPI api, PayloadPlugin payloadPlugin, Plugin plugin) {
        this.api = api;
        this.payloadPlugin = payloadPlugin;
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(DatabaseName.class).toInstance("Database");
        bind(PayloadDatabaseService.class).in(Singleton.class);
        bind(ServerManager.class);
    }

    @Provides
    @Singleton
    PayloadDatabase provideDatabase(PayloadDatabaseService databaseService) {
        return databaseService.fromConfigFile(plugin, "database.yml", "Database");
    }

    @Provides
    Morphia provideMorphia(PayloadDatabase database) {
        return database.getMorphia();
    }

    @Provides
    Datastore provideDatastore(PayloadDatabase database) {
        return database.getDatastore();
    }

    @Provides
    MongoClient provideMongoClient(PayloadDatabase database) {
        return database.getMongoClient();
    }

    @Provides
    JedisPool provideJedisPool(PayloadDatabase database) {
        return database.getJedisPool();
    }



}
