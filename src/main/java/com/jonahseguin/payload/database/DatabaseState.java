package com.jonahseguin.payload.database;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DatabaseState {

    private volatile boolean mongoConnected = false;
    private volatile boolean redisConnected = false;

    private volatile boolean mongoConnecting = false;

    /**
     * Check the connectivity of both databases
     * @return true if both are connected
     */
    public boolean isDatabaseConnected() {
        return this.mongoConnected && this.redisConnected;
    }


}
