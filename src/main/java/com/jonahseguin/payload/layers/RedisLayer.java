package com.jonahseguin.payload.layers;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.exception.CachingException;
import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.type.CacheSource;
import com.jonahseguin.payload.type.CacheStage;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSONParseException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class RedisLayer<T extends Profile> extends CacheLayer<T, T, CachingProfile<T>> {

    private final String REDIS_KEY_PREFIX = "purified.profile."; // purified.profile.<UUID>
    private Jedis jedis = null;
    private final Class<T> clazz;

    public RedisLayer(ProfileCache<T> cache, CacheDatabase database, Class<T> clazz) {
        super(cache, database);
        this.clazz = clazz;
    }

    @Override
    public T provide(CachingProfile<T> passable) {
        try {
            passable.setLoadingSource(this.source());
            passable.setStage(CacheStage.LOADED);
            return get(passable.getUniqueId());
        } catch (Exception ex) {
            return cache.getFailureHandler().providerException(this, passable, ex);
        }
    }

    @Override
    public T get(String uniqueId) {
        String json = jedis.get(REDIS_KEY_PREFIX + uniqueId);
        if (json != null) {
            return mapProfile(json); // Can be null
        } else {
            return null;
        }
    }

    @Override
    public boolean save(T profilePassable) {
        String json = jsonifyProfile(profilePassable);
        if (json != null) {
            try {
                jedis.set(REDIS_KEY_PREFIX + profilePassable.getUniqueId(), json);
                return true;
            } catch (Exception ex) {
                super.getCache().getDebugger().error(ex, "An exception occurred while attempting to save profile to Redis");
                return false;
            }
        } else {
            // Failed to convert Profile to JSON; cannot save
            return false;
        }
    }

    @Override
    public boolean has(String uniqueId) {
        try {
            return jedis.exists(uniqueId);
        } catch (JedisException ex) {
            super.getCache().getDebugger().error(ex, "An exception occurred with Jedis while attempting has/exists call");
            return false;
        }
    }

    @Override
    public boolean remove(String uniqueId) {
        try {
            jedis.del(REDIS_KEY_PREFIX + uniqueId); // returns long --> code reply?  not sure if it's the amount deleted
            return true;
        } catch (JedisException ex) {
            super.getCache().getDebugger().error(ex, "An exception occurred with Jedis while attempting to remove a Profile from Redis cache");
            return false;
        }

    }

    @Override
    public boolean init() {
        try {
            this.jedis = database.getJedis();
            return true;
        } catch (Exception expected) {
            super.getCache().getDebugger().error(expected, "Redis Cache Layer failed to init");
            return false;
        }
    }

    @Override
    public boolean shutdown() {
        try {
            if (jedis != null) {
                jedis.close();
                jedis = null;
            }
            return true;
        } catch (Exception expected) {
            super.getCache().getDebugger().error(expected, "Redis Cache Layer failed to shutdown");
            return false;
        }
    }

    @Override
    public CacheSource source() {
        return CacheSource.REDIS;
    }

    @Override
    public int cleanup() {
        // TODO
        return 0;
    }

    @Override
    public int clear() {
        jedis.flushDB();
        return -1;
    }

    private String jsonifyProfile(T profile) {
        try {
            BasicDBObject dbObject = (BasicDBObject) database.getMorphia().toDBObject(profile);
            return dbObject.toJson();
        } catch (JSONParseException ex) {
            super.getCache().getDebugger().error(ex, "Could not parse JSON while trying to convert Profile to JSON for Redis");
        } catch (Exception ex) {
            super.getCache().getDebugger().error(ex, "Could not convert Profile to JSON for Redis");
        }
        return null;
    }

    private T mapProfile(String json) {
        try {
            DBObject dbObject = BasicDBObject.parse(json);
            T profile = database.getMorphia().fromDBObject(database.getDatastore(), clazz, dbObject);
            if (profile != null) {
                return profile;
            } else {
                throw new CachingException("Profile to map cannot be null");
            }
        } catch (JSONParseException ex) {
            super.getCache().getDebugger().error(ex, "Could not parse JSON to map profile from Redis");
        } catch (Exception ex) {
            super.getCache().getDebugger().error(ex, "Could not map profile from Redis");
        }
        return null;
    }

}
