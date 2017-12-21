package com.jonahseguin.payload.profile.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.caching.ProfileLayerResult;
import com.jonahseguin.payload.profile.event.PayloadProfilePreSaveEvent;
import com.jonahseguin.payload.profile.event.PayloadProfileSavedEvent;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.Profile;
import com.jonahseguin.payload.profile.profile.SimpleProfilePassable;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.profile.type.PCacheStage;
import com.mongodb.MongoException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mongodb.morphia.query.Query;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class PMongoLayer<T extends Profile> extends ProfileCacheLayer<T, T, CachingProfile<T>> {

    private final Class<T> clazz;
    
    public PMongoLayer(PayloadProfileCache<T> cache, CacheDatabase database, Class<T> clazz) {
        super(cache, database);
        this.clazz = clazz;
    }

    @Override
    public T provide(CachingProfile<T> passable) {
        try {
            passable.setLoadingSource(this.source());
            passable.setStage(PCacheStage.LOADED);
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

            // Call Pre-Save Event
            PayloadProfilePreSaveEvent<T> preSaveEvent = new PayloadProfilePreSaveEvent<>(profilePassable, getCache(), source());
            getPlugin().getServer().getPluginManager().callEvent(preSaveEvent);
            profilePassable = preSaveEvent.getProfile();

            database.getDatastore().save(profilePassable);

            // Call Saved Event
            PayloadProfileSavedEvent<T> savedEvent = new PayloadProfileSavedEvent<>(profilePassable, getCache(), source());
            getPlugin().getServer().getPluginManager().callEvent(savedEvent);

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
        // No init necessary as it is done for us by the provided database
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
    public PCacheSource source() {
        return PCacheSource.MONGO;
    }

    @Override
    public int cleanup() {
        int cleaned = 0;
        for (Player pl : getCache().getPlugin().getServer().getOnlinePlayers()) {
            Profile profile = getCache().getLocalProfile(pl);
            if (profile == null) {
                cleaned++;
                pl.sendMessage(ChatColor.RED + "You appear to have no profile loaded.  We will now attempt to load a profile for you.");
                CachingProfile<T> cachingProfile;
                if (getCache().getLayerController().getPreCachingLayer().has(pl.getUniqueId().toString())) {
                    cachingProfile = getCache().getLayerController().getPreCachingLayer().get(pl.getUniqueId().toString());
                } else {
                    ProfileLayerResult<CachingProfile<T>> result = getCache().getExecutorHandler().preCachingExecutor(SimpleProfilePassable.fromPlayer(pl)).execute();
                    if (result.isSuccess()) {
                        cachingProfile = result.getResult();
                    } else {
                        // Cannot load if they cannot get a caching profile... have to kick them
                        pl.kickPlayer(ChatColor.RED + "You did not have a profile loaded and a fatal error occurred while attempting to get one for you.\n"
                                + ChatColor.RED + "This should not happen.  Please contact an administrator and attempt to re-log.");
                        continue;
                    }
                }
                getCache().getFailureHandler().startFailureHandling(cachingProfile);
            }
        }

        // Check for null usernames
        {
            Query<T> q = database.getDatastore().createQuery(clazz);
            q.criteria("name").equal(null);
            cleaned += getDatabase().getDatastore().delete(q).getN();
        }

        // Check for null UUIDs
        {
            Query<T> q = database.getDatastore().createQuery(clazz);
            q.criteria("uniqueId").equal(null);
            cleaned += getDatabase().getDatastore().delete(q).getN();
        }


        return cleaned;
    }

    @Override
    public int clear() {
        throw new UnsupportedOperationException("Cannot clear the MongoDB database using this method.");
    }
}
