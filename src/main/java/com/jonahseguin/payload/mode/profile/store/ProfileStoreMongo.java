/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.store;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.base.type.PayloadQueryModifier;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import com.mongodb.MongoException;
import dev.morphia.query.Query;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

public class ProfileStoreMongo<X extends PayloadProfile> extends ProfileCacheStore<X> {

    private final Set<PayloadQueryModifier<X>> queryModifiers = new HashSet<>();
    private boolean running = false;

    public ProfileStoreMongo(ProfileCache<X> cache) {
        super(cache);
    }

    @Override
    public Optional<X> get(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        try {
            Query<X> q = getQuery(key);
            Stream<X> stream = q.find().toList().stream();
            return stream.findFirst();
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error getting Profile from MongoDB Layer: " + key.toString());
            return Optional.empty();
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error getting Profile from MongoDB Layer: " + key.toString());
            return Optional.empty();
        }
    }

    @Override
    public boolean has(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        try {
            return getQuery(uuid).find().toList().stream().findAny().isPresent();
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error check if Profile exists in MongoDB Layer: " + uuid.toString());
            return false;
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error checking if Profile exists in MongoDB Layer: " + uuid.toString());
            return false;
        }
    }

    @Override
    public void remove(@Nonnull UUID key) {
        Preconditions.checkNotNull(key);
        try {
            Query<X> q = getQuery(key);
            cache.getDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error removing Profile from MongoDB Layer: " + key.toString());
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error removing Profile from MongoDB Layer: " + key.toString());
        }
    }

    @Override
    public Optional<X> get(@Nonnull ProfileData data) {
        Preconditions.checkNotNull(data);
        return get(data.getUniqueId());
    }

    public Optional<X> getByUsername(@Nonnull String username) {
        Preconditions.checkNotNull(username);
        try {
            Query<X> q = getQueryForUsername(username);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            X x = xp.orElse(null);
            if (x != null) {
                x.interact();
                x.setLoadingSource(layerName());
            }
            return xp;
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error getting Profile from MongoDB Layer: " + username);
            return Optional.empty();
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error getting Profile from MongoDB Layer: " + username);
            return Optional.empty();
        }
    }

    @Override
    public boolean save(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        payload.interact();
        try {
            cache.getDatabase().getDatastore().save(payload);
            return true;
        } catch (MongoException ex) {
            getCache().getErrorService().capture(ex, "MongoDB error saving Profile to MongoDB Layer: " + payload.getUsername());
            return false;
        } catch (Exception expected) {
            getCache().getErrorService().capture(expected, "Error saving Profile to MongoDB Layer: " + payload.getUsername());
            return false;
        }
    }

    @Override
    public boolean has(@Nonnull ProfileData data) {
        Preconditions.checkNotNull(data);
        return has(data.getUniqueId());
    }

    @Override
    public boolean has(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        return has(payload.getUniqueId());
    }

    @Override
    public void remove(@Nonnull ProfileData data) {
        Preconditions.checkNotNull(data);
        remove(data.getUniqueId());
    }

    @Override
    public void remove(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        remove(payload.getUniqueId());
    }


    @Override
    public int cleanup() {
        int cleaned = 0;

        // Check for online players that don't have profiles
        for (Player player : cache.getPlugin().getServer().getOnlinePlayers()) {
            Optional<X> o = cache.getFromCache(player);
            if (o.isPresent()) {
                X payload = o.get();
                // They don't have a profile but are online
                // Check if they are already failure handling
                ProfileData data = cache.getData(player.getUniqueId());
                if (data != null) {
                    if (cache.getFailureManager().hasFailure(data)) {
                        continue; // They are already being handled
                    }
                }

                if (data == null) {
                    data = cache.createData(player.getName(), player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
                }

                // They aren't failure handling if we got here, let them know of the issue and start failure handling
                player.sendMessage(cache.getLang().module(cache).format("no-profile"));
                cache.getFailureManager().fail(data);
            }
        }

        return cleaned;
    }

    @Override
    @Nonnull
    public Collection<X> getAll() {
        return createQuery().find().toList();
    }

    @Override
    public long clear() {
        // For safety reasons...
        throw new UnsupportedOperationException("Cannot clear a MongoDB database from within Payload.");
    }

    @Override
    public long size() {
        return cache.getDatabase().getDatastore().find(cache.getPayloadClass()).count();
    }

    @Override
    public boolean start() {
        boolean success = true;
        if (!cache.getDatabase().isRunning()) {
            cache.getErrorService().capture("Error initializing MongoDB Profile Layer: Payload Database is not connected");
            success = false;
        }
        if (cache.getSettings().isServerSpecific()) {
            addCriteriaModifier(query -> query.field("payloadId").equalIgnoreCase(cache.getApi().getPayloadID()));
        }
        running = true;
        return success;
    }

    @Override
    public boolean shutdown() {
        running = false;
        // Do nothing here.  MongoDB object closing will be handled when the cache shuts down.
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Nonnull
    @Override
    public String layerName() {
        return "Profile MongoDB";
    }

    public void addCriteriaModifier(PayloadQueryModifier<X> modifier) {
        queryModifiers.add(modifier);
    }

    public void removeCriteriaModifier(PayloadQueryModifier<X> modifier) {
        queryModifiers.remove(modifier);
    }

    public void applyQueryModifiers(Query<X> query) {
        for (PayloadQueryModifier<X> modifier : queryModifiers) {
            modifier.apply(query);
        }
    }

    public Query<X> createQuery() {
        Query<X> q = cache.getDatabase().getDatastore().createQuery(cache.getPayloadClass());
        applyQueryModifiers(q);
        return q;
    }

    public Query<X> getQuery(UUID uniqueId) {
        Query<X> q = createQuery();
        q.criteria("uniqueId").equalIgnoreCase(uniqueId.toString());
        return q;
    }

    public Query<X> getQueryForUsername(String username) {
        Query<X> q = createQuery();
        q.criteria("username").equalIgnoreCase(username);
        return q;
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

}
