package com.jonahseguin.payload.profile.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.common.exception.CachingException;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.Profile;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.profile.type.PCacheStage;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSONParseException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

public class PRedisLayer<T extends Profile> extends ProfileCacheLayer<T, T, CachingProfile<T>> {

    private final String REDIS_KEY_PREFIX; // <key>.profile.<UUID>
    private Jedis jedis = null;
    private final Class<T> clazz;

    public PRedisLayer(PayloadProfileCache<T> cache, CacheDatabase database, Class<T> clazz) {
        super(cache, database);
        this.clazz = clazz;
        this.REDIS_KEY_PREFIX = cache.getSettings().getRedisKeyPrefix() + ".profile.";
    }

    @Override
    public T provide(CachingProfile<T> passable) {
        try {
            passable.setLoadingSource(this.source());
            passable.setStage(PCacheStage.LOADED);
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
            super.getCache().getDebugger().error(expected, "Redis ProfileCache Layer failed to init");
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
            super.getCache().getDebugger().error(expected, "Redis ProfileCache Layer failed to shutdown");
            return false;
        }
    }

    @Override
    public PCacheSource source() {
        return PCacheSource.REDIS;
    }

    @Override
    public int cleanup() {
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
