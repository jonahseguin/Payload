package com.jonahseguin.payload.mode.profile.layer;

import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import com.mongodb.MongoException;
import org.bukkit.entity.Player;
import org.mongodb.morphia.query.Query;
import redis.clients.jedis.Jedis;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ProfileLayerMongo<X extends PayloadProfile> extends ProfileCacheLayer<X> {

    private Jedis jedis = null;

    public ProfileLayerMongo(ProfileCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(ProfileData data) throws PayloadLayerCannotProvideException {
        if (!this.has(data)) {
            throw new PayloadLayerCannotProvideException("Cannot provide (does not have) in MongoDB layer for Profile username:" + data.getUsername(), this.cache);
        }
        try {
            Query<X> q = getQuery(data.getUniqueId());
            Stream<X> stream = q.asList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.orElseThrow(() -> new PayloadLayerCannotProvideException("Failed to provide Profile from Mongo layer for username:" + data.getUsername(), this.cache));
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error getting Profile from MongoDB Layer: " + data.getUsername());
            return null;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error getting Profile from MongoDB Layer: " + data.getUsername());
            return null;
        }
    }

    @Override
    public boolean save(X payload) {
        payload.interact();
        try {
            this.cache.getPayloadDatabase().getDatastore().save(payload);
            return true;
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error saving Profile to MongoDB Layer: " + payload.getUsername());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error saving Profile to MongoDB Layer: " + payload.getUsername());
            return false;
        }
    }

    @Override
    public boolean has(ProfileData data) {
        try {
            Query<X> q = getQuery(data.getUniqueId());
            Stream<X> stream = q.asList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error check if Profile exists in MongoDB Layer: " + data.getUsername());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Profile exists in MongoDB Layer: " + data.getUsername());
            return false;
        }
    }

    @Override
    public boolean has(X payload) {
        payload.interact();
        try {
            Query<X> q = getQuery(payload.getUniqueId());
            Stream<X> stream = q.asList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error checking if Profile exists in MongoDB Layer: " + payload.getUsername());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Profile exists in MongoDB Layer: " + payload.getUsername());
            return false;
        }
    }

    @Override
    public void remove(ProfileData data) {
        try {
            Query<X> q = getQuery(data.getUniqueId());
            this.cache.getPayloadDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error removing Profile from MongoDB Layer: " + data.getUsername());
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error removing Profile from MongoDB Layer: " + data.getUsername());
        }
    }

    @Override
    public void remove(X payload) {
        try {
            Query<X> q = getQuery(payload.getUniqueId());
            this.cache.getPayloadDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error removing Profile from MongoDB Layer: " + payload.getUsername());
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error removing Profile from MongoDB Layer: " + payload.getUsername());
        }
    }

    @Override
    public int cleanup() {
        int cleaned = 0;
        for (Player player : this.cache.getPlugin().getServer().getOnlinePlayers()) {
            X payload = this.cache.getLocalProfile(player);
            if (payload == null) {

                // They don't have a profile but aren't online
                // Check if they are already failure handling
                ProfileData data = this.cache.getData(player.getUniqueId());
                if (data != null) {
                    if (this.cache.getFailureManager().hasFailure(data)) {
                        continue; // They are already being handled
                    }
                }

                if (data == null) {
                    data = this.cache.createData(player.getName(), player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
                }

                // They aren't failure handling if we got here, let them know of the issue and start failure handling
                player.sendMessage(this.cache.getLangController().get(PLang.PLAYER_ONLINE_NO_PROFILE));
                this.cache.getFailureManager().fail(data);
            }
        }

        return cleaned;
    }

    @Override
    public int clear() {
        int size = this.jedis.hkeys(this.getCache().getName()).size();
        this.jedis.del(this.getCache().getName());
        return size;
    }

    @Override
    public void init() {
        if (!this.cache.getPayloadDatabase().isStarted()) {
            this.cache.getErrorHandler().error(this.cache, "Error initializing MongoDB Profile Layer: Payload Database is not started");
        }
    }

    @Override
    public void shutdown() {
        // Do nothing here.  MongoDB object closing will be handled when the cache shuts down.
    }

    @Override
    public String layerName() {
        return "Profile MongoDB";
    }

    public Query<X> getQuery(UUID uniqueId) {
        Query<X> q = this.cache.getPayloadDatabase().getDatastore().createQuery(this.cache.getPayloadClass());
        q.maxTime(10, TimeUnit.SECONDS);
        q.criteria("uniqueId").equalIgnoreCase(uniqueId.toString());
        return q;
    }

    public Query<X> getQueryForUsername(String username) {
        Query<X> q = this.cache.getPayloadDatabase().getDatastore().createQuery(this.cache.getPayloadClass());
        q.maxTime(10, TimeUnit.SECONDS);
        q.criteria("username").equalIgnoreCase(username);
        return q;
    }

}
