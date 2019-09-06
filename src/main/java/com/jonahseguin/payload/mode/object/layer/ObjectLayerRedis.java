package com.jonahseguin.payload.mode.object.layer;

import com.jonahseguin.payload.base.exception.PayloadException;
import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.ObjectData;
import com.mongodb.BasicDBObject;
import com.mongodb.util.JSONParseException;
import lombok.Getter;
import redis.clients.jedis.Jedis;

import java.util.Map;

@Getter
public class ObjectLayerRedis<X extends Payload> extends ObjectCacheLayer<X> {

    public ObjectLayerRedis(ObjectCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(String key) throws PayloadLayerCannotProvideException {
        if (!this.has(key)) {
            throw new PayloadLayerCannotProvideException("Cannot provide in local layer for object identifier: " + key, this.cache);
        }
        return this.localCache.get(key);
    }

    @Override
    public X get(ObjectData data) throws PayloadLayerCannotProvideException {
        return this.get(data.getIdentifier());
    }

    @Override
    public boolean save(X payload) {
        this.localCache.put(payload.getIdentifier(), payload);
        return true;
    }

    @Override
    public boolean has(String key) {
        return this.localCache.containsKey(key);
    }

    @Override
    public boolean has(ObjectData data) {
        return this.has(data.getIdentifier());
    }

    @Override
    public boolean has(X payload) {
        return this.has(payload.getIdentifier());
    }

    @Override
    public void remove(String key) {
        this.localCache.remove(key);
    }

    @Override
    public void remove(ObjectData data) {
        this.remove(data.getIdentifier());
    }

    @Override
    public void remove(X payload) {
        this.remove(payload.getIdentifier());
    }

    @Override
    public int cleanup() {
        if (this.cache.getSettings().getRedisExpiryTimeSeconds() > 0) {
            final long expiredTimestamp = System.currentTimeMillis() - (1000 * this.cache.getSettings().getRedisExpiryTimeSeconds());
            Map<String, String> objectsString = this.jedis().hgetAll(this.cache.getName());
            int cleaned = 0;
            for (Map.Entry<String, String> entry : objectsString.entrySet()) {
                try {
                    X object = mapObject(entry.getValue());
                    if (object != null) {
                        if (object.getRedisCacheTimestamp() <= expiredTimestamp) {
                            // Object is expired, remove it from Redis
                            this.jedis().hdel(this.cache.getName(), entry.getKey());
                        }
                    } else {
                        // If the object is null, let's remove it from Redis (if it couldn't map or is just null)
                        this.jedis().hdel(this.cache.getName(), entry.getKey());
                    }
                } catch (Exception ex) {
                    // If we get an error [are unable to map the profile], let's remove it from Redis.
                    this.jedis().hdel(this.cache.getName(), entry.getKey());
                }
            }
            return cleaned;
        }
        return 0;
    }

    @Override
    public long clear() {
        long size = this.jedis().hlen(this.cache.getName());
        this.jedis().del(this.cache.getName());
        return size;
    }

    @Override
    public void init() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public String layerName() {
        return "Object Redis";
    }

    @Override
    public long size() {
        return this.jedis().hlen(this.cache.getName());
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

    private Jedis jedis() {
        return this.cache.getPayloadDatabase().getJedis();
    }

    private String jsonifyObject(X object) {
        try {
            BasicDBObject dbObject = (BasicDBObject) this.getCache().getPayloadDatabase().getMorphia().toDBObject(object);
            return dbObject.toJson();
        } catch (JSONParseException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "Could not parse JSON while trying to convert Object to JSON for Redis");
        } catch (Exception ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "Could not convert Object to JSON for Redis");
        }
        return null;
    }

    private X mapObject(String json) {
        try {
            BasicDBObject dbObject = BasicDBObject.parse(json);
            X object = this.getCache().getPayloadDatabase().getMorphia().fromDBObject(this.getCache().getPayloadDatabase().getDatastore(), this.getCache().getPayloadClass(), dbObject);
            if (object != null) {
                return object;
            } else {
                throw new PayloadException("Object to map cannot be null", this.getCache());
            }
        } catch (JSONParseException ex) {
            super.getCache().getErrorHandler().exception(this.getCache(), ex, "Could not parse JSON to map object from Redis");
        } catch (Exception ex) {
            super.getCache().getErrorHandler().exception(this.getCache(), ex, "Could not map object from Redis");
        }
        return null;
    }

}
