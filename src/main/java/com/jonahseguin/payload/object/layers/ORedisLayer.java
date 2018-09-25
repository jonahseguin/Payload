package com.jonahseguin.payload.object.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.exception.CachingException;
import com.jonahseguin.payload.common.exception.PayloadException;
import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.event.ObjectPreSaveEvent;
import com.jonahseguin.payload.object.event.ObjectSavedEvent;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.type.OLayerType;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSONParseException;
import redis.clients.jedis.Jedis;

public class ORedisLayer<X extends ObjectCacheable> extends ObjectCacheLayer<X> {

    private Jedis jedis = null;
    private final Class<X> clazz;

    public ORedisLayer(PayloadObjectCache<X> cache, CacheDatabase database, Class<X> clazz) {
        super(cache, database);
        this.clazz = clazz;
    }

    @Override
    public X provide(String id) {
        if (!cache.getSettings().isUseRedis()) {
            throw new CachingException("Cannot use Redis layer when useRedis is disabled!");
        }
        String json = jedis.hget(cache.getSettings().getRedisKey(), id);
        if (json != null) {
            return mapObject(json);
        }
        else {
            return null;
        }
    }

    @Override
    public boolean save(String id, X x) {
        if (!cache.getSettings().isUseRedis()) {
            throw new CachingException("Cannot use Redis layer when useRedis is disabled!");
        }
        if (!x.persist()) return false;

        // Pre-Save Event
        ObjectPreSaveEvent<X> preSaveEvent = new ObjectPreSaveEvent<>(x, source(), cache);
        getPlugin().getServer().getPluginManager().callEvent(preSaveEvent);
        x = preSaveEvent.getObject();

        String json = jsonifyObject(x);
        jedis.hset(cache.getSettings().getRedisKey(), id, json);

        // Saved Event
        ObjectSavedEvent<X> savedEvent = new ObjectSavedEvent<>(x, source(), cache);
        getPlugin().getServer().getPluginManager().callEvent(savedEvent);

        return true;
    }

    @Override
    public boolean has(String id) {
        if (!cache.getSettings().isUseRedis()) {
            throw new CachingException("Cannot use Redis layer when useRedis is disabled!");
        }
        return jedis.hexists(cache.getSettings().getRedisKey(), id);
    }

    @Override
    public boolean remove(String id) {
        if (!cache.getSettings().isUseRedis()) {
            throw new CachingException("Cannot use Redis layer when useRedis is disabled!");
        }
        jedis.hdel(cache.getSettings().getRedisKey(), id);
        return true;
    }

    @Override
    public boolean init() {
        if (!cache.getSettings().isUseRedis()) {
            throw new CachingException("Cannot use Redis layer when useRedis is disabled!");
        }

        // Ensure the redis key has been set for this Payload instance
        if (this.cache.getSettings().getRedisKey().equalsIgnoreCase("payload")) {
            super.getCache().getDebugger().error(new PayloadException("Redis key must be changed from default 'payload' value!"));
            return false;
        }

        try {
            jedis = database.getJedis();
            return true;
        }
        catch (Exception ex) {
            error(ex);
            return false;
        }
    }

    @Override
    public boolean shutdown() {
        if (!cache.getSettings().isUseRedis()) {
            throw new CachingException("Cannot use Redis layer when useRedis is disabled!");
        }
        if (jedis != null) {
            jedis.close();
            jedis = null;
        }
        return true;
    }

    @Override
    public OLayerType source() {
        return OLayerType.REDIS;
    }

    @Override
    public int cleanup() {
        return 0;
    }

    @Override
    public int clear() {
        final int size = jedis.hkeys(this.cache.getSettings().getRedisKey()).size();
        jedis.del(this.cache.getSettings().getRedisKey());
        return size;
    }

    private String jsonifyObject(X obj) {
        try {
            BasicDBObject dbObject = (BasicDBObject) database.getMorphia().toDBObject(obj);
            return dbObject.toJson();
        } catch (JSONParseException ex) {
            super.getCache().getDebugger().error(ex, "Could not parse JSON while trying to convert Object to JSON for Redis");
        } catch (Exception ex) {
            super.getCache().getDebugger().error(ex, "Could not convert Object to JSON for Redis");
        }
        return null;
    }

    private X mapObject(String json) {
        try {
            DBObject dbObject = BasicDBObject.parse(json);
            X obj = database.getMorphia().fromDBObject(database.getDatastore(), clazz, dbObject);
            if (obj != null) {
                return obj;
            } else {
                throw new CachingException("Object to map cannot be null");
            }
        } catch (JSONParseException ex) {
            super.getCache().getDebugger().error(ex, "Could not parse JSON to map Object from Redis");
        } catch (Exception ex) {
            super.getCache().getDebugger().error(ex, "Could not map Object from Redis");
        }
        return null;
    }

}
