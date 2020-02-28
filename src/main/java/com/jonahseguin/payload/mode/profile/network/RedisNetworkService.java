/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.network;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.mongodb.BasicDBObject;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

public class RedisNetworkService<X extends PayloadProfile> implements NetworkService<X> {

    private final ProfileCache<X> cache;
    private final DatabaseService database;
    private boolean running = false;

    public RedisNetworkService(ProfileCache<X> cache) {
        this.cache = cache;
        this.database = cache.getDatabase();
    }

    @Override
    public Optional<NetworkProfile> get(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid, "UUID cannot be null");
        final String hashKey = cache.getServerSpecificName();
        final String keyString = cache.keyToString(uuid);
        Preconditions.checkNotNull(hashKey, "Hash key cannot be null");
        Preconditions.checkNotNull(keyString, "Key cannot be null");
        try {
            if (database.getRedis().sync().hexists(hashKey, keyString)) {
                final String json = database.getRedis().sync().hget(hashKey, keyString);
                if (json != null && json.length() > 0) {
                    BasicDBObject dbObject = BasicDBObject.parse(json);
                    NetworkProfile networkProfile = database.getMorphia().fromDBObject(database.getDatastore(), NetworkProfile.class, dbObject);
                    return Optional.ofNullable(networkProfile);
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error getting network payload from Key in Redis Network Service");
            return Optional.empty();
        }
    }

    @Override
    public Optional<NetworkProfile> get(@Nonnull X payload) {
        Preconditions.checkNotNull(payload, "Payload cannot be null");
        final String hashKey = cache.getServerSpecificName();
        final String keyString = cache.keyToString(payload.getIdentifier());
        Preconditions.checkNotNull(hashKey, "Hash key cannot be null");
        Preconditions.checkNotNull(keyString, "Key cannot be null");
        try {
            if (database.getRedis().sync().hexists(hashKey, keyString)) {
                final String json = database.getRedis().sync().hget(hashKey, keyString);
                if (json != null && json.length() > 0) {
                    BasicDBObject dbObject = BasicDBObject.parse(json);
                    NetworkProfile networkProfile = database.getMorphia().fromDBObject(database.getDatastore(), NetworkProfile.class, dbObject);
                    if (networkProfile != null) {
                        networkProfile.setIdentifier(payload.getIdentifier());
                        return Optional.of(networkProfile);
                    } else {
                        return createForGet(payload);
                    }
                } else {
                    return createForGet(payload);
                }
            } else {
                return createForGet(payload);
            }
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error getting network payload from Payload in Redis Network Service");
            return Optional.empty();
        }
    }

    private Optional<NetworkProfile> createForGet(@Nonnull X payload) {
        NetworkProfile networkProfile = create(payload);
        if (!save(networkProfile)) {
            cache.getErrorService().capture("Failed to save after creation of network payload " + cache.keyToString(payload.getIdentifier()));
        }
        return Optional.of(networkProfile);
    }

    @Override
    public boolean has(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid, "UUID cannot be null");
        final String hashKey = cache.getServerSpecificName();
        final String keyString = cache.keyToString(uuid);
        Preconditions.checkNotNull(hashKey, "Hash key cannot be null");
        Preconditions.checkNotNull(keyString, "Key cannot be null");
        try {
            return database.getRedis().sync().hexists(hashKey, keyString);
        } catch (Exception ex) {
            cache.getErrorService().capture("Error checking if hexists in Redis Network Service for UUID:" + keyString);
        }
        return false;
    }

    @Override
    public boolean save(@Nonnull NetworkProfile networkProfile) {
        Preconditions.checkNotNull(networkProfile, "NetworkProfile cannot be null");
        final String hashKey = cache.getServerSpecificName();
        final String keyString = cache.keyToString(networkProfile.getIdentifier());
        Preconditions.checkNotNull(hashKey, "Hash key cannot be null");
        Preconditions.checkNotNull(keyString, "Key cannot be null");
        BasicDBObject object = (BasicDBObject) database.getMorphia().toDBObject(networkProfile);
        if (object != null) {
            String json = object.toJson();
            Preconditions.checkNotNull(json, "JSON cannot be null");
            Preconditions.checkNotNull(cache.getServerSpecificName(), "Server specific cache name cannot be null");
            Preconditions.checkNotNull(networkProfile.getIdentifier(), "Payload identifier cannot be null");
            Preconditions.checkNotNull(cache.keyToString(networkProfile.getIdentifier()), "Payload identifier key cannot be null");
            try {
                database.getRedis().async().hset(hashKey, keyString, json);
                return true;
            } catch (Exception ex) {
                cache.getErrorService().capture(ex, "Error saving NetworkProfile in Redis Network Service for UUID: " + cache.keyToString(networkProfile.getIdentifier()));
                return false;
            }
        }
        return false;
    }

    @Override
    public Optional<X> get(@Nonnull NetworkProfile payload) {
        Preconditions.checkNotNull(payload);
        return cache.get(payload.getIdentifier());
    }

    @Override
    public NetworkProfile create(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        NetworkProfile np = cache.createNetworked();
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
