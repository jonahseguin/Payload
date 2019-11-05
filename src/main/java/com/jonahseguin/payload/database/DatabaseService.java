/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.server.ServerService;
import com.mongodb.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nonnull;
import java.util.Set;

public interface DatabaseService extends Service {

    MongoClient getMongoClient();

    Morphia getMorphia();

    Datastore getDatastore();

    Jedis getJedisResource();

    Jedis getMonitorJedis();

    JedisPool getJedisPool();

    ServerService getServerService();

    boolean isConnected();

    boolean canFunction(@Nonnull DatabaseDependent dependent);

    String getName();

    void hook(DatabaseDependent dependent);

    Set<DatabaseDependent> getHooks();

    ErrorService getErrorService();

    DatabaseState getState();

    boolean connectRedis();



}
