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

@Getter
public class ObjectLayerRedis<X extends Payload> extends ObjectCacheLayer<X> {

    public ObjectLayerRedis(ObjectCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(String key) throws PayloadLayerCannotProvideException {
        try {
            String json = this.jedis().hget(this.getCache().getName(), key);
            return mapObject(json);
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error getting Object from Redis Layer: " + key);
            return null;
        }
    }

    @Override
    public X get(ObjectData data) throws PayloadLayerCannotProvideException {
        return this.get(data.getIdentifier());
    }

    @Override
    public boolean save(X payload) {
        String json = jsonifyObject(payload);
        try {
            this.jedis().hset(this.getCache().getName(), payload.getIdentifier(), json);
            return true;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error saving Object to Redis Layer: " + payload.getIdentifier());
            return false;
        }
    }

    @Override
    public boolean has(String key) {
        try {
            return this.jedis().hexists(this.getCache().getName(), key);
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Object exists in Redis Layer: " + key);
            return false;
        }
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
        try {
            this.jedis().hdel(this.getCache().getName(), key);
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error removing Object from Redis Layer: " + key);
        }
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
