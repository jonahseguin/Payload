/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.store;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileCache;
import lombok.Getter;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ProfileStoreLocal<X extends PayloadProfile> extends ProfileCacheStore<X> {

    private final ConcurrentMap<UUID, X> localCache = new ConcurrentHashMap<>();
    private boolean running = false;

    public ProfileStoreLocal(PayloadProfileCache<X> cache) {
        super(cache);
    }

    @Override
    public Optional<X> get(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        X x = localCache.get(uuid);
        if (x != null) {
            x.interact();
            x.setLoadingSource(layerName());
        }
        return Optional.ofNullable(x);
    }

    public X getByObjectID(ObjectId id) {
        return localCache.values().stream().filter(x -> x.getObjectId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public boolean save(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        payload.interact();
        localCache.put(payload.getUniqueId(), payload);
        return true;
    }

    @Override
    public boolean has(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        return localCache.containsKey(key);
    }

    @Override
    public void remove(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        localCache.remove(key);
    }

    @Override
    public boolean has(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        payload.interact();
        return localCache.containsKey(payload.getUniqueId());
    }


    @Override
    public void remove(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        localCache.remove(payload.getUniqueId());
    }

    @Override
    public int cleanup() {
        long expiryTimeSeconds = getCache().getSettings().getLocalExpiryTimeSeconds();
        Set<UUID> purge = new HashSet<>();
        for (Map.Entry<UUID, X> entry : localCache.entrySet()) {
            if (!entry.getValue().isOnline() && entry.getValue().getLastInteractionTimestamp() < (System.currentTimeMillis() - (expiryTimeSeconds * 1000))) {
                // Expired
                purge.add(entry.getKey());
            }
        }

        for (UUID key : purge) {
            cache.uncache(key);
        }

        return purge.size();
    }

    @Override
    public long clear() {
        long i = localCache.size();
        localCache.clear();
        return i;
    }

    @Nonnull
    @Override
    public Collection<X> getAll() {
        return localCache.values();
    }

    @Override
    public long size() {
        return localCache.size();
    }

    @Override
    public boolean start() {
        // Nothing to initialize
        running = true;
        return true;
    }

    @Override
    public boolean shutdown() {
        clear(); // For memory purposes
        running = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Nonnull
    @Override
    public String layerName() {
        return "Profile Local";
    }

    @Override
    public boolean isDatabase() {
        return false;
    }
}
