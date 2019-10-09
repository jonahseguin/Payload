/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.layer;

import com.jonahseguin.payload.base.exception.PayloadException;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import com.mongodb.BasicDBObject;
import redis.clients.jedis.Jedis;

import java.util.*;

public class ProfileLayerRedis<X extends PayloadProfile> extends ProfileCacheLayer<X> {

    public ProfileLayerRedis(ProfileCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(UUID uuid) {
        try (Jedis jedis = cache.getPayloadDatabase().getResource()) {
            String json = jedis.hget(this.getCache().getServerSpecificName(), uuid.toString());
            if (json == null) {
                return null;
            }
            X x = mapProfile(json);
            if (x != null) {
                x.interact();
                x.interactRedis();
                x.setLoadingSource(this.layerName());
            }
            return x;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error getting Profile from Redis Layer: " + uuid.toString());
            return null;
        }
    }

    @Override
    public boolean has(UUID uuid) {
        try (Jedis jedis = cache.getPayloadDatabase().getResource()) {
            return jedis.hexists(this.getCache().getServerSpecificName(), uuid.toString());
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Profile exists in Redis Layer: " + uuid.toString());
            return false;
        }
    }

    @Override
    public void remove(UUID uuid) {
        try (Jedis jedis = cache.getPayloadDatabase().getResource()) {
            jedis.hdel(this.getCache().getServerSpecificName(), uuid.toString());
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error removing Profile from Redis Layer: " + uuid);
        }
    }

    @Override
    public X get(ProfileData data) {
        return this.get(data.getUniqueId());
    }

    @Override
    public boolean save(X payload) {
        payload.interact();
        payload.interactRedis();
        String json = jsonifyProfile(payload);
        try (Jedis jedis = cache.getPayloadDatabase().getResource()) {
            jedis.hset(this.getCache().getServerSpecificName(), payload.getUniqueId().toString(), json);
            return true;
        }
        catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error saving Profile to Redis Layer: " + payload.getUsername());
            return false;
        }
    }

    @Override
    public boolean has(ProfileData data) {
        return this.has(data.getUniqueId());
    }

    @Override
    public boolean has(X payload) {
        return this.has(payload.getUniqueId());
    }

    @Override
    public void remove(ProfileData data) {
        this.remove(data.getUniqueId());
    }

    @Override
    public void remove(X payload) {
        this.remove(payload.getUniqueId());
    }

    @Override
    public int cleanup() {
        final int cacheRedisExpirySeconds = this.cache.getSettings().getRedisExpiryTimeSeconds(); // TTL
        // if a Profile has a cachedTime of <= expiredTimestamp, remove
        final long expiredTimestamp = System.currentTimeMillis() - (1000 * cacheRedisExpirySeconds);
        int cleaned = 0;
        try (Jedis jedis = this.cache.getPayloadDatabase().getResource()) {
            Map<String, String> objectsString = jedis.hgetAll(this.cache.getServerSpecificName());
            for (Map.Entry<String, String> entry : objectsString.entrySet()) {
                try {
                    X object = mapProfile(entry.getValue());
                    if (object != null) {
                        if (object.getRedisCacheTimestamp() <= expiredTimestamp) {
                            // Object is expired, remove it from Redis
                            jedis.hdel(this.cache.getServerSpecificName(), entry.getKey());
                        }
                    } else {
                        // If the object is null, let's remove it from Redis (if it couldn't map or is just null)
                        jedis.hdel(this.cache.getServerSpecificName(), entry.getKey());
                    }
                } catch (Exception ex) {
                    // If we get an error [are unable to map the profile], let's remove it from Redis.
                    jedis.hdel(this.cache.getServerSpecificName(), entry.getKey());
                }
            }
        }
        return cleaned;
    }

    @Override
    public long clear() {
        try (Jedis jedis = this.cache.getPayloadDatabase().getResource()) {
            long size = jedis.hlen(this.getCache().getServerSpecificName());
            jedis.del(this.getCache().getServerSpecificName());
            return size;
        } catch (Exception ex) {
            this.cache.getErrorHandler().exception(this.getCache(), ex, "Error clearing all Profiles from Redis Layer");
            return 0;
        }
    }

    @Override
    public Collection<X> getAll() {
        try (Jedis jedis = this.cache.getPayloadDatabase().getResource()) {
            Map<String, String> entrySet = jedis.hgetAll(this.cache.getServerSpecificName());
            Set<X> profiles = new HashSet<>();
            for (String json : entrySet.values()) {
                profiles.add(mapProfile(json));
            }
            return profiles;
        } catch (Exception ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "Error getting all Profiles from Redis Layer");
            return null;
        }
    }
    
    @Override
    public long size() {
        try (Jedis jedis = this.cache.getPayloadDatabase().getResource()) {
            return jedis.hlen(this.getCache().getServerSpecificName());
        } catch (Exception ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "Error getting size from Redis Layer");
            return 0;
        }
    }

    @Override
    public void init() {

    }

    @Override
    public void shutdown() {
        // Do nothing here.  jedis object closing will be handled when the cache shuts down.
    }

    private String jsonifyProfile(X profile) {
        try {
            profile.interact();
            BasicDBObject dbObject = (BasicDBObject) this.getCache().getPayloadDatabase().getMorphia().toDBObject(profile);
            return dbObject.toJson();
        } catch (Exception ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "Could not convert PayloadProfile to JSON for Redis");
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
        } catch (Exception ex) {
            super.getCache().getErrorHandler().exception(this.getCache(), ex, "Could not map profile from Redis");
        }
        return null;
    }

    @Override
    public String layerName() {
        return "Profile Redis";
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

}
