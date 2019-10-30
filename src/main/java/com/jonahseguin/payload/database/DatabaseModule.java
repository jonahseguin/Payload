/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
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
        bind(DatabaseService.class).to(PayloadDatabaseService.class);
    }

    @Provides
    Morphia provideMorphia(DatabaseService database) {
        return database.getMorphia();
    }

    @Provides
    Datastore provideDatastore(DatabaseService database) {
        return database.getDatastore();
    }

    @Provides
    MongoClient provideMongoClient(DatabaseService database) {
        return database.getMongoClient();
    }

    @Provides
    JedisPool provideJedisPool(DatabaseService database) {
        return database.getJedisPool();
    }

    @Provides
    ServerManager provideServerManager(DatabaseService database) {
        return database.getServerManager();
    }


}
