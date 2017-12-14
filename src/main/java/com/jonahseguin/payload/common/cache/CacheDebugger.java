package com.jonahseguin.payload.common.cache;

/**
 * Created by Jonah on 11/14/2017.
 * Project: Payload
 *
 * @ 9:39 PM
 *
 * Debugger to be implemented by the plugin using Payload; logging and error handling and startup failure handling
 *
 */
public interface CacheDebugger {

    /**
     * Called for debug messages from Payload
     * @param message String
     */
    void debug(String message);

    /**
     * Called when a generic exception occurs within Payload
     * @param ex Exception
     */
    void error(Exception ex);

    /**
     * Called when a specific exception occurs within Payload and a message is provided by Payload
     * @param ex Exception
     * @param message String
     */
    void error(Exception ex, String message);

    /**
     * Called by Payload when the cache fails to initialize during startup
     * @return boolean: whether or not shutdown the cache
     */
    boolean onStartupFailure();

}
