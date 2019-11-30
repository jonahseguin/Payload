/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.network;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.database.DatabaseService;
import com.mongodb.BasicDBObject;
import redis.clients.jedis.Jedis;

import javax.annotation.Nonnull;
import java.util.Optional;

public class RedisNetworkService<K, X extends Payload<K>, N extends NetworkPayload<K>, D> implements NetworkService<K, X, N> {

    private final PayloadCache<K, X, N> cache;
    private final DatabaseService database;
    private final Class<N> type;
    private boolean running = false;

    @Inject
    public RedisNetworkService(PayloadCache<K, X, N> cache, Class<N> type, DatabaseService database) {
        this.cache = cache;
        this.database = database;
        this.type = type;
    }

    @Override
    public Optional<N> get(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        try (Jedis jedis = database.getJedisResource()) {
            String json = jedis.hget(cache.getServerSpecificName(), cache.keyToString(key));
            if (json != null) {
                BasicDBObject dbObject = BasicDBObject.parse(json);
                N np = database.getMorphia().fromDBObject(database.getDatastore(), type, dbObject);
                return Optional.ofNullable(np);
            }
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error getting network payload in Redis Network Service");
        }
        return Optional.empty();
    }

    @Override
    public Optional<N> get(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        try (Jedis jedis = database.getJedisResource()) {
            String json = jedis.hget(cache.getServerSpecificName(), cache.keyToString(payload.getIdentifier()));
            if (json != null && json.length() > 0) {
                BasicDBObject dbObject = BasicDBObject.parse(json);
                N np = database.getMorphia().fromDBObject(database.getDatastore(), type, dbObject);
                if (np != null) {
                    np.setIdentifier(payload.getIdentifier());
                }
                return Optional.ofNullable(np);
            } else {
                N np = create(payload);
                if (save(np)) {
                    return Optional.of(np);
                } else {
                    cache.getErrorService().capture("Failed to save after creation of network payload " + cache.keyToString(payload.getIdentifier()));
                }
            }
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error getting network payload in Redis Network Service");
        }
        return Optional.empty();
    }

    @Override
    public boolean has(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        try (Jedis jedis = database.getJedisResource()) {
            return jedis.hexists(cache.getServerSpecificName(), cache.keyToString(key));
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error checking if hexists() in Redis Network Service: " + cache.keyToString(key));
        }
        return false;
    }

    @Override
    public boolean save(@Nonnull N payload) {
        Preconditions.checkNotNull(payload);
        BasicDBObject object = (BasicDBObject) database.getMorphia().toDBObject(payload);
        if (object != null) {
            String json = object.toJson();
            Preconditions.checkNotNull(json, "JSON is null");
            Preconditions.checkNotNull(cache.getServerSpecificName(), "Server specific cache name is null");
            Preconditions.checkNotNull(payload.getIdentifier());
            Preconditions.checkNotNull(cache.keyToString(payload.getIdentifier()), "Payload identifier key is null");
            try (Jedis jedis = database.getJedisResource()) {
                jedis.hset(cache.getServerSpecificName(), cache.keyToString(payload.getIdentifier()), json);
                return true;
            } catch (Exception ex) {
                cache.getErrorService().capture(ex, "Error saving in Redis Network Service: " + cache.keyToString(payload.getIdentifier()));
            }
        }
        return false;
    }

    @Override
    public Optional<X> get(@Nonnull N payload) {
        Preconditions.checkNotNull(payload);
        return cache.get(payload.getIdentifier());
    }

    @Override
    public N create(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        N np = cache.createNetworked();
        np.setIdentifier(payload.getIdentifier());
        return np;
    }

    @Override
    public boolean start() {
        running = true;
        return true;
    }

    @Override
    public boolean shutdown() {
        running = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
