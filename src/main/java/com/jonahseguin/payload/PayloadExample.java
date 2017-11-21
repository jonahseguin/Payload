package com.jonahseguin.payload;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.CacheDebugger;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.caching.CachingController;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.util.CacheBuilder;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import redis.clients.jedis.Jedis;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 7:44 PM
 */
public class PayloadExample extends JavaPlugin {

    private boolean debug = true; // example variable
    private boolean maintenanceMode = false; // example variable

    /* Database Example Variables */
    private MongoClient mongoClient; // example variable
    private MongoDatabase mongoDatabase; // example variable
    private Jedis jedis; // example variable
    private Morphia morphia; // example variable
    private Datastore datastore; // example variable

    @Getter
    private ProfileCache<PProfile> cache = null; // Our profile cache.  Will be loaded in onEnable

    @Override
    public void onEnable() {
        this.cache = setupCache();

        if (!this.cache.init()) { // Startup the cache
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        PProfile profile = this.cache.getSimpleCache().getProfileByUsername("Shawckz");
        if (profile.getPlayer() != null && profile.getPlayer().isOnline()) {
            profile.getPlayer().sendMessage(ChatColor.GREEN + "You have " + profile.getBalance() + "dollars!");
        }

        // Manual caching -- This is a basic example of how the internal caching is handled
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            final Player player = Bukkit.getPlayerExact("Shawckz");
            CachingController<PProfile> controller = this.cache.getController(player);
            PProfile loaded = controller.cache(); // Manual
            controller.join(player); // Finish initializing their profile
            loaded.setBalance(500);
            cache.saveEverywhere(profile);
            // Or if you wanted to save to a specific layer (redis/mongo/local):
            cache.getLayerController().getRedisLayer().save(loaded); // Save to just redis
            player.sendMessage("Your balance is now 500.");
        });
    }

    @Override
    public void onDisable() {
        if (this.cache != null) {
            if (!this.cache.shutdown()) {
                getLogger().severe("The cache failed to shutdown properly!");
            }
        }
    }

    private ProfileCache<PProfile> setupCache() {
        // Use the Cache Builder to setup our cache
        return new CacheBuilder<PProfile>(this)
                .withProfileClass(PProfile.class) // Our Profile
                .withCacheLocalExpiryMinutes(30) // Local profiles expire after 30 mins of being inactive (i.e logging out)
                .withCacheLogoutSaveDatabase(true) // Save their profile to *Mongo* (and always redis) when they logout
                .withCacheRemoveOnLogout(false) // Don't remove them from the local cache when they logout
                .withHaltListenerEnabled(true) // Allow Payload to handle the halt listener
                .withCacheFailRetryIntervalSeconds(30) // Re-try caching for fails every 30 seconds
                .withDatabase(new CacheDatabase(mongoClient, mongoDatabase, jedis, morphia, datastore)) // Pass in our database properties
                .withDebugger(new CacheDebugger() { // Handle the debug and errors provided by Payload

                    @Override
                    public void debug(String message) {
                        if (debug) {
                            getLogger().info("[Debug][Cache] " + message);
                        }
                    }

                    @Override
                    public void error(Exception ex) {
                        getLogger().info("[Error][Cache] An exception occurred: " + ex.getMessage());
                        if (debug) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public void error(Exception ex, String message) {
                        getLogger().info("[Error][Cache] An exception occurred: " + message);
                        if (debug) {
                            ex.printStackTrace();
                        }
                    }

                    @Override
                    public boolean onStartupFailure() {
                        maintenanceMode = true; // Enable maintenance mode to prevent more players from joining

                        for (Player pl : getServer().getOnlinePlayers()) {
                            if (!pl.isOp()) {
                                pl.kickPlayer(ChatColor.RED + "The server is experiencing technical difficulties.\n" +
                                "Please join back later.");
                            }
                            else {
                                pl.sendMessage(ChatColor.RED + "All players were kicked due to a cache startup failure.");
                            }
                        }

                        return true; // Shutdown cache if it fails to start
                    }
                })
                .withInstantiator((username, uniqueId) -> new PProfile(username, uniqueId, 200)) // This handles the instantiation of our Profile when a new one is created
                .build(); // Done
    }

    // Our Profile class implementation (you should probably put it in it's own file but this is just an example!)
    @Entity("example_profiles")
    @Getter
    @Setter
    public class PProfile extends Profile {

        private int balance = 0; // Automatically mapped to/from MongoDB via Morphia
        // If you wanted a field to not be included in Morphia mapping, use transient or @Transient
        // More info is available on the Morphia documentation

        public PProfile() {
            // You have to have an empty constructor to ensure Morphia can instantiate
        }

        public PProfile(String name, String uniqueId) {
            super(name, uniqueId);
        }

        public PProfile(int balance) {
            this.balance = balance;
        }

        public PProfile(String name, String uniqueId, int balance) {
            super(name, uniqueId);
            this.balance = balance;
        }
    }

}
