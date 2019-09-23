package com.jonahseguin.payload.database.redis;

import redis.clients.jedis.Jedis;

public interface SafeRedisHook {

    /**
     * Called when the resource is provided by Jedis
     * Could be after the connection was reset and the pool was re-created
     *
     * @param jedis Jedis resource from pool
     */
    void withResource(Jedis jedis);

}
