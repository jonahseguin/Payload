/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.server.ServerService;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.ext.guice.GuiceExtension;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nonnull;
import java.util.Set;

@Singleton
public class PayloadDatabaseService implements DatabaseService {

    private final String name;
    private final ErrorService error;
    private final Morphia morphia;
    private final PayloadDatabase database;
    private Datastore datastore = null;
    private ServerService serverService = null;

    @Inject
    public PayloadDatabaseService(Injector injector, @Database String name, @Database ErrorService error, PayloadDatabase database) {
        this.name = name;
        this.error = error;
        this.morphia = new Morphia();
        this.database = database;

        new GuiceExtension(morphia, injector);
        initDatastore();
    }

    @Override
    public boolean isRunning() {
        return database.isRunning();
    }

    @Override
    public MongoClient getMongoClient() {
        return database.getMongoClient();
    }

    @Override
    public Morphia getMorphia() {
        return morphia;
    }

    @Override
    public MongoDatabase getDatabase() {
        return database.getDatabase();
    }

    @Override
    public Datastore getDatastore() {
        return datastore;
    }

    @Override
    public Jedis getJedisResource() {
        return database.getJedisPool().getResource();
    }

    @Override
    public JedisPool getJedisPool() {
        return database.getJedisPool();
    }

    @Override
    public boolean isConnected() {
        return database.getState().isDatabaseConnected();
    }

    @Override
    public boolean canFunction(@Nonnull DatabaseDependent dependent) {
        Preconditions.checkNotNull(dependent);
        return database.getState().canCacheFunction(dependent);
    }

    @Override
    public ServerService getServerService() {
        return serverService;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Jedis getMonitorJedis() {
        return database.getMonitorJedis();
    }

    @Override
    public Set<DatabaseDependent> getHooks() {
        return database.getHooks();
    }

    @Override
    public ErrorService getErrorService() {
        return error;
    }

    @Override
    public DatabaseState getState() {
        return database.getState();
    }

    @Override
    public void hook(DatabaseDependent cache) {
        if (!this.database.getHooks().contains(cache)) {
            this.database.getHooks().add(cache);
        } else {
            throw new IllegalStateException("Payload Database '" + name + "' has already hooked cache '" + cache + "'");
        }
    }

    @Override
    public boolean start() {
        boolean success = database.start();
        if (success) {
            initDatastore();
        }
        return success;
    }

    private void initDatastore() {
        if (datastore == null) {
            if (morphia != null && database.getMongoClient() != null) {
                datastore = morphia.createDatastore(database.getMongoClient(), database.getName());
                datastore.ensureIndexes();
            }
        }
    }

    @Override
    public boolean shutdown() {
        return database.shutdown();
    }

}
