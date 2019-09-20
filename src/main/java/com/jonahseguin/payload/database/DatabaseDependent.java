package com.jonahseguin.payload.database;

import com.jonahseguin.payload.base.error.PayloadErrorHandler;
import com.jonahseguin.payload.base.type.Payload;

public interface DatabaseDependent {

    /**
     * Called when MongoDB loses an active connection
     */
    void onMongoDbDisconnect();

    /**
     * Called when Redis loses an active connection
     */
    void onRedisDisconnect();

    /**
     * Called when MongoDB regains a connection after it was previously lost
     */
    void onMongoDbReconnect();

    /**
     * Called when Redis regains a connection after it was previously lost
     */
    void onRedisReconnect();

    /**
     * Called when MongoDB connects for this first time (ex. during startup)
     */
    void onMongoDbInitConnect();

    /**
     * Called when Redis connects for this first time (ex. during startup)
     */
    void onRedisInitConnect();

    /**
     * Does this database-dependent object depend on Redis for functionality?
     * @return True if required
     */
    boolean requireRedis();

    /**
     * Does this database-dependent object depend on MongoDB for functionality?
     * @return True if required
     */
    boolean requireMongoDb();

    PayloadErrorHandler getErrorHandler();

}
