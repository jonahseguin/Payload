/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;


import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.database.mongo.PayloadMongo;
import com.jonahseguin.payload.database.redis.PayloadRedis;
import com.jonahseguin.payload.database.redis.PayloadRedisMonitor;
import com.jonahseguin.payload.server.ServerService;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Set;

public interface PayloadDatabase {

    boolean start();

    boolean shutdown();

    boolean connectMongo();

    boolean connectRedis();

    DatabaseState getState();

    String getName();

    Plugin getPlugin();

    ErrorService getErrorService();

    PayloadPlugin getPayloadPlugin();

    Set<DatabaseDependent> getHooks();

    ServerService getServerService();

    boolean isRunning();

    PayloadMongo getPayloadMongo();

    MongoClient getMongoClient();

    MongoDatabase getDatabase();

    PayloadRedis getPayloadRedis();

    JedisPool getJedisPool();

    Jedis getMonitorJedis();

    PayloadRedisMonitor getRedisMonitor();

}
