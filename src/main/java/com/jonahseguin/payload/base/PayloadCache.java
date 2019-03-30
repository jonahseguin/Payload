package com.jonahseguin.payload.base;

import org.bukkit.plugin.Plugin;

public interface PayloadCache {

    /**
     * Starts up & initializes the cache.
     * Prepares everything for a fresh startup, ensures database connections, etc.
     * @return boolean: true if successful, false if any errors encountered
     */
    boolean init();

    /**
     * Shut down the cache.
     * Saves everything first, and safely shuts down
     * @return boolean: true if successful, false if any errors encountered
     */
    boolean shutdown();

    /**
     * Randomly generated UUID for this cache
     * @return String: Cache ID
     */
    String getCacheId();

    /**
     * Get the name of this cache (set by the end user, should be unique)
     * @return String: Cache Name
     */
    String getName();

    /**
     * Get the JavaPlugin controlling this cache
     * Every Payload cache must be associated with a JavaPlugin for event handling and etc.
     * @return Plugin
     */
    Plugin getPlugin();

}
