package com.jonahseguin.payload.mode.profile.layer;

import com.jonahseguin.payload.base.exception.PayloadException;
import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSONParseException;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class ProfileLayerRedis<X extends PayloadProfile> extends ProfileCacheLayer<X> {

    private Jedis jedis = null;

    public ProfileLayerRedis(ProfileCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(ProfileData data) throws PayloadLayerCannotProvideException {
        if (!this.has(data)) {
            throw new PayloadLayerCannotProvideException("Cannot provide (does not have) in local layer for Profile username:" + data.getUsername(), this.cache);
        }
        String json = jedis.hget(this.getCache().getName(), data.getUniqueId());
        return mapProfile(json);
    }

    @Override
    public boolean save(X payload) {
        payload.interact();
        payload.interactRedis();
        this.localCache.put(payload.getUniqueId(), payload);
        return true;
    }

    @Override
    public boolean has(ProfileData data) {
        return this.localCache.containsKey(data.getUniqueId());
    }

    @Override
    public boolean has(X payload) {
        payload.interact();
        return this.localCache.containsKey(payload.getUniqueId());
    }

    @Override
    public void remove(ProfileData data) {
        this.localCache.remove(data.getUniqueId());
    }

    @Override
    public void remove(X payload) {
        this.localCache.remove(payload.getUniqueId());
    }

    @Override
    public int cleanup() {
        final int cacheRedisExpirySeconds = this.cache.getSettings().getRedisExpiryTimeSeconds(); // TTL
        // if a Profile has a cachedTime of <= expiredTimestamp, remove
        final long expiredTimestamp = System.currentTimeMillis() - (1000 * cacheRedisExpirySeconds);
        Map<String, String> objectsString = jedis.hgetAll(this.cache.getName());
        int cleaned = 0;
        for (Map.Entry<String, String> entry : objectsString.entrySet()) {
            try {
                X object = mapProfile(entry.getValue());
                if (object != null) {
                    if (object.getRedisCacheTimestamp() <= expiredTimestamp) {
                        // Object is expired, remove it from Redis
                        jedis.hdel(this.cache.getName(), entry.getKey());
                    }
                } else {
                    // If the object is null, let's remove it from Redis (if it couldn't map or is just null)
                    jedis.hdel(this.cache.getName(), entry.getKey());
                }
            } catch (Exception ex) {
                // If we get an error [are unable to map the profile], let's remove it from Redis.
                jedis.hdel(this.cache.getName(), entry.getKey());
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
        try {
            this.jedis = this.getCache().getPayloadDatabase().getJedis();
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache().getName(), expected, "Error initializing Jedis in Profile Redis Layer");
        }
    }

    @Override
    public void shutdown() {
        // Do nothing here.  Jedis object closing will be handled when the cache shuts down.
    }

    private String jsonifyProfile(X profile) {
        try {
            profile.interact();
            BasicDBObject dbObject = (BasicDBObject) this.getCache().getPayloadDatabase().getMorphia().toDBObject(profile);
            return dbObject.toJson();
        } catch (JSONParseException ex) {
            this.getCache().getErrorHandler().exception(this.getCache().getName(), ex, "Could not parse JSON while trying to convert PayloadProfile to JSON for Redis");
        } catch (Exception ex) {
            this.getCache().getErrorHandler().exception(this.getCache().getName(), ex, "Could not convert PayloadProfile to JSON for Redis");
        }
        return null;
    }

    private X mapProfile(String json) {
        try {
            BasicDBObject dbObject = BasicDBObject.parse(json);
            X profile = this.getCache().getPayloadDatabase().getMorphia().fromDBObject(this.getCache().getPayloadDatabase().getDatastore(), this.getCache().getPayloadClass(), dbObject);
            if (profile != null) {
                return profile;
            } else {
                throw new PayloadException("PayloadProfile to map cannot be null", this.getCache());
            }
        } catch (JSONParseException ex) {
            super.getCache().getErrorHandler().exception(this.getCache().getName(), ex, "Could not parse JSON to map profile from Redis");
        } catch (Exception ex) {
            super.getCache().getErrorHandler().exception(this.getCache().getName(), ex, "Could not map profile from Redis");
        }
        return null;
    }

}
