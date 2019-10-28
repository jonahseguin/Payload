/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.mongodb.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public interface DatabaseService {

    MongoClient getMongoClient();

    Morphia getMorphia();

    Datastore getDatastore();

    Jedis getJedisResource();

    JedisPool getJedisPool();

}
