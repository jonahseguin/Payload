package com.jonahseguin.payload.common.cache;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.Data;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import redis.clients.jedis.Jedis;

/**
 * Created by Jonah on 11/14/2017.
 * Project: Payload
 *
 * @ 9:44 PM
 *
 * This class is a consumer object storage class that is provided by the plugin that is using Payload
 *
 */
@Data
public class CacheDatabase {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final Jedis jedis;
    private final Morphia morphia;
    private final Datastore datastore;

    public boolean isMongoConnected() {
        try {
            mongoClient.getAddress();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
