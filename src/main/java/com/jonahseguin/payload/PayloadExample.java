package com.jonahseguin.payload;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.CacheDebugger;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.util.CacheBuilder;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Entity;
import redis.clients.jedis.Jedis;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 7:44 PM
 */
public class PayloadExample extends JavaPlugin {

    private boolean debug = true;
    private boolean maintenanceMode = false;

    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private Jedis jedis;
    private Morphia morphia;
    private Datastore datastore;

    @Entity("example_profiles")
    @Getter @Setter
    public class PProfile extends Profile {
        private int balance = 0;

        public PProfile() {
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

    private ProfileCache<PProfile> cache = null;

    @Override
    public void onEnable() {
        // TODO: Setup local database variables for cache to use...

        this.cache = setupCache();

        PProfile profile = this.cache.getSimpleCache().getProfileByUsername("Shawckz");
        if (profile.getPlayer() != null && profile.getPlayer().isOnline()) {
            profile.getPlayer().sendMessage(ChatColor.GREEN + "You have " + profile.getBalance() + "dollars!");
        }

    }

    private ProfileCache<PProfile> setupCache() {
        return new CacheBuilder<PProfile>(this)
                .withProfileClass(PProfile.class)
                .withCacheLocalExpiryMinutes(30)
                .withCacheLogoutSaveDatabase(true)
                .withDatabase(new CacheDatabase(mongoClient, mongoDatabase, jedis, morphia, datastore))
                .withDebugger(new CacheDebugger() {

                    @Override
                    public void debug(String message) {
                        getLogger().info("[Debug][Cache] " + message);
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
                .withInstantiator((username, uniqueId) -> new PProfile(username, uniqueId, 200))
                .build();
    }

}
