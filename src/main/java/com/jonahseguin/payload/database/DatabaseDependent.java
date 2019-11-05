package com.jonahseguin.payload.database;

public interface DatabaseDependent {

    /**
     * Called when MongoDB loses an active connection
     */
    default void onMongoDbDisconnect() {
    }

    /**
     * Called when Redis loses an active connection
     */
    default void onRedisDisconnect() {
    }

    /**
     * Called when MongoDB regains a connection after it was previously lost
     */
    default void onMongoDbReconnect() {
    }

    /**
     * Called when Redis regains a connection after it was previously lost
     */
    default void onRedisReconnect() {
    }

    /**
     * Called when MongoDB connects for this first time (ex. during startup)
     */
    default void onMongoDbInitConnect() {
    }

    /**
     * Called when Redis connects for this first time (ex. during startup)
     */
    default void onRedisInitConnect() {
    }

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


}
