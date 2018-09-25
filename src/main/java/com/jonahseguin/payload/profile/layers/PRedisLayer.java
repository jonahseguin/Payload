package com.jonahseguin.payload.profile.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.exception.CachingException;
import com.jonahseguin.payload.common.exception.PayloadException;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.event.PayloadProfilePreSaveEvent;
import com.jonahseguin.payload.profile.event.PayloadProfileSavedEvent;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.profile.type.PCacheStage;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSONParseException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Map;

public class PRedisLayer<T extends PayloadProfile> extends ProfileCacheLayer<T, T, CachingProfile<T>> {

    private final String REDIS_KEY;
    private final Class<T> clazz;
    private Jedis jedis = null;

    public PRedisLayer(PayloadProfileCache<T> cache, CacheDatabase database, Class<T> clazz) {
        super(cache, database);
        this.clazz = clazz;
        this.REDIS_KEY = cache.getSettings().getRedisKey();
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
        String json = jedis.hget(this.REDIS_KEY, uniqueId);
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

                // Call Pre-Save Event
                PayloadProfilePreSaveEvent<T> preSaveEvent = new PayloadProfilePreSaveEvent<>(profilePassable, getCache(), source());
                getPlugin().getServer().getPluginManager().callEvent(preSaveEvent);
                profilePassable = preSaveEvent.getProfile();

                // Save profile to Redis in the hash at the specified key
                jedis.hset(this.REDIS_KEY, profilePassable.getUniqueId(), json);

                // Call Saved Event
                PayloadProfileSavedEvent<T> savedEvent = new PayloadProfileSavedEvent<>(profilePassable, getCache(), source());
                getPlugin().getServer().getPluginManager().callEvent(savedEvent);

                return true;
            } catch (Exception ex) {
                super.getCache().getDebugger().error(ex, "An exception occurred while attempting to save profile to Redis");
                return false;
            }
        } else {
            // Failed to convert PayloadProfile to JSON; cannot save
            return false;
        }
    }

    @Override
    public boolean has(String uniqueId) {
        if (uniqueId == null || uniqueId.length() <= 0) {
            return false;
        }
        try {
            return jedis.hexists(this.REDIS_KEY, uniqueId);
        } catch (JedisException ex) {
            super.getCache().getDebugger().error(ex, "An exception occurred with Jedis while attempting has/exists call");
            return false;
        }
    }

    @Override
    public boolean remove(String uniqueId) {
        if (uniqueId == null) {
            return false;
        }
        try {
            jedis.hdel(this.REDIS_KEY, uniqueId);
            return true;
        } catch (JedisException ex) {
            super.getCache().getDebugger().error(ex, "An exception occurred with Jedis while attempting to remove a PayloadProfile from Redis cache");
            return false;
        }

    }

    @Override
    public boolean init() {
        // Ensure the redis key has been set for this Payload instance
        if (this.REDIS_KEY.equalsIgnoreCase("payload")) {
            super.getCache().getDebugger().error(new PayloadException("Redis key must be changed from default 'payload' value!"));
            return false;
        }

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
        final int cacheRedisExpiryMinutes = this.cache.getSettings().getCacheRedisExpiryMinutes(); // TTL
        // if a Profile has a cachedTime of <= expiredTimestamp, remove
        final long expiredTimestamp = System.currentTimeMillis() - (1000 * 60 * cacheRedisExpiryMinutes);
        Map<String, String> objectsString = jedis.hgetAll(this.REDIS_KEY);
        int cleaned = 0;
        for (Map.Entry<String, String> entry : objectsString.entrySet()) {
            try {
                T object = mapProfile(entry.getValue());
                if (object != null) {
                    if (object.getRedisCacheTime() <= expiredTimestamp) {
                        // Object is expired, remove it from Redis
                        jedis.hdel(this.REDIS_KEY, entry.getKey());
                    }
                }
                else {
                    // If the object is null, let's remove it from Redis (if it couldn't map or is just null)
                    jedis.hdel(this.REDIS_KEY, entry.getKey());
                }
            }
            catch (Exception ex) {
                // If we get an error [are unable to map the profile], let's remove it from Redis.
                jedis.hdel(this.REDIS_KEY, entry.getKey());
            }
        }
        return cleaned;
    }

    /**
     * WARNING: Using this method will completely clear the cache from Redis [only for this plugin/project]
     * @return The amount of keys removed from our cache
     */
    @Override
    public int clear() {
        final int size = jedis.hkeys(this.REDIS_KEY).size();
        jedis.del(this.REDIS_KEY);
        return size;
    }

    private String jsonifyProfile(T profile) {
        try {
            profile.setRedisCacheTime(System.currentTimeMillis());
            BasicDBObject dbObject = (BasicDBObject) database.getMorphia().toDBObject(profile);
            return dbObject.toJson();
        } catch (JSONParseException ex) {
            super.getCache().getDebugger().error(ex, "Could not parse JSON while trying to convert PayloadProfile to JSON for Redis");
        } catch (Exception ex) {
            super.getCache().getDebugger().error(ex, "Could not convert PayloadProfile to JSON for Redis");
        }
        return null;
    }

    private T mapProfile(String json) {
        try {
            BasicDBObject dbObject = BasicDBObject.parse(json);
            T profile = database.getMorphia().fromDBObject(database.getDatastore(), clazz, dbObject);
            if (profile != null) {
                return profile;
            } else {
                throw new CachingException("PayloadProfile to map cannot be null");
            }
        } catch (JSONParseException ex) {
            super.getCache().getDebugger().error(ex, "Could not parse JSON to map profile from Redis");
        } catch (Exception ex) {
            super.getCache().getDebugger().error(ex, "Could not map profile from Redis");
        }
        return null;
    }

}
