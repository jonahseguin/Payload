package com.jonahseguin.payload.layers;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.type.CacheSource;
import com.jonahseguin.payload.type.CacheStage;
import com.mongodb.MongoException;
import org.mongodb.morphia.query.Query;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MongoLayer<T extends Profile> extends CacheLayer<T, T, CachingProfile<T>> {

    private final Class<T> clazz;
    
    public MongoLayer(ProfileCache<T> cache, CacheDatabase database, Class<T> clazz) {
        super(cache, database);
        this.clazz = clazz;
    }

    @Override
    public T provide(CachingProfile<T> passable) {
        try {
            passable.setLoadingSource(this.source());
            passable.setStage(CacheStage.LOADED);
            Query<T> q = getQuery(passable.getUniqueId());
            Stream<T> stream = q.asList().stream();
            Optional<T> xp = stream.findFirst();
            return xp.orElse(null);
        } catch (Exception ex) {
            return getCache().getFailureHandler().providerException(this, passable, ex);
        }
    }

    @Override
    public T get(String uniqueId) {
        try {
            Query<T> q = getQuery(uniqueId);
            Stream<T> stream = q.asList().stream();
            Optional<T> xp = stream.findFirst();
            return xp.orElse(null);
        } catch (MongoException ex) {
            error(ex, "An exception occurred with MongoDB while trying to get a profile");
        } catch (Exception ex) {
            error(ex, "An exception occurred while trying to get a profile from the MongoDB database");
        }
        return null;
    }

    public T getByUsername(String username) {
        try {
            Query<T> q = getQueryForUsername(username);
            Stream<T> stream = q.asList().stream();
            Optional<T> xp = stream.findFirst();
            return xp.orElse(null);
        } catch (MongoException ex) {
            error(ex, "An exception occurred with MongoDB while trying to get a profile");
        } catch (Exception ex) {
            error(ex, "An exception occurred while trying to get a profile from the MongoDB database");
        }
        return null;
    }

    @Override
    public boolean save(T profilePassable) {
        try {
            if (profilePassable.isTemporary()) return false;
            database.getDatastore().save(profilePassable);
            return true;
        } catch (MongoException ex) {
            error(ex, "A MongoDB exception occurred while trying to save profile: " + profilePassable.getName());
        } catch (Exception ex) {
            error(ex, "Could not save profile to MongoDB: " + profilePassable.getName());
        }
        return false;
    }

    public Query<T> getQuery(String uniqueId) {
        Query<T> q = database.getDatastore().createQuery(clazz);
        q.maxTime(10, TimeUnit.SECONDS);
        q.criteria("uniqueId").equalIgnoreCase(uniqueId);
        return q;
    }

    public Query<T> getQueryForUsername(String username) {
        Query<T> q = database.getDatastore().createQuery(clazz);
        q.maxTime(10, TimeUnit.SECONDS);
        q.criteria("name").equalIgnoreCase(username);
        return q;
    }

    @Override
    public boolean has(String uniqueId) {
        try {
            Query<T> q = getQuery(uniqueId);
            Stream<T> stream = q.asList().stream();
            Optional<T> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            error(ex, "A MongoDB exception occurred while checking if profile is in MongoDB");
        } catch (Exception ex) {
            error(ex, "Could not check if profile is in MongoDB");
        }
        return false;
    }

    @Override
    public boolean remove(String uniqueId) {
        try {
            Query<T> q = getQuery(uniqueId);
            T profile = getDatabase().getDatastore().findAndDelete(q);
            return profile != null;
        } catch (MongoException ex) {
            error(ex, "A MongoDB exception occurred while deleting profile from MongoDB");
        } catch (Exception ex) {
            error(ex, "Could not remove (delete) profile from MongoDB");
        }
        return false;
    }

    @Override
    public boolean init() {
        // No init necessary as it is done for us by the DatabaseService
        return true;
    }

    @Override
    public boolean shutdown() {
        int errors = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            T profile = getCache().getProfile(player);
            if (profile != null) {
                // Save only here (Mongo)
                if (!this.save(profile)) {
                    player.sendMessage(ChatColor.RED + "Failed to save your profile during shutdown.  Please notify an administrator of this error.");
                    debug(format("&c{0}'s profile failed to save during shutdown. (FATAL ERROR: potential data loss)"));
                    errors++;
                }
            } else {
                // Player is online without a loaded Profile... this should not happen
                player.sendMessage(ChatColor.RED + "You do not appear to have a loaded profile.  Please notify an administrator of this error.");
                debug(format("&c{0} is online without a loaded profile.  This should not happen. (shutdown)", player.getName()));
                errors++;
            }
        }
        debug(ChatColor.RED + "[FATAL ERROR] There were " + errors + " errors during MongoDB shutdown involving profile saving.");

        return true;
    }

    @Override
    public CacheSource source() {
        return CacheSource.MONGO;
    }

    @Override
    public int cleanup() {
        // TODO: Check for players that are online but DO NOT have a loaded Profile
        // TODO: Check for null players, etc.
        return 0;
    }

    @Override
    public int clear() {
        throw new UnsupportedOperationException("Cannot clear the MongoDB database using this method.");
    }
}
