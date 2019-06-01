package com.jonahseguin.payload.database;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseState {

    private volatile boolean mongoConnected = false;
    private volatile boolean redisConnected = false;

    /**
     * Check the connectivity of both databases
     * @return true if both are connected
     */
    public boolean isDatabaseConnected() {
        return this.mongoConnected && this.redisConnected;
    }

    public boolean canCacheFunction(DatabaseDependent dependent) {
        boolean mongo = true;
        if (dependent.requireMongoDb()) {
            if (!this.mongoConnected) {
                mongo = false;
            }
        }
        boolean redis = true;
        if (dependent.requireRedis()) {
            if (!this.redisConnected) {
                redis = false;
            }
        }
        return mongo && redis;
    }

}
