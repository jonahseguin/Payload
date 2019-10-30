/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.layer;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.type.PayloadQueryModifier;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import com.mongodb.MongoException;
import dev.morphia.query.Query;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;

public class ProfileLayerMongo<X extends PayloadProfile> extends ProfileCacheLayer<X> {

    private final Set<PayloadQueryModifier<X>> queryModifiers = new HashSet<>();

    public ProfileLayerMongo(ProfileCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(UUID key) {
        try {
            Query<X> q = getQuery(key);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.orElse(null);
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error getting Profile from MongoDB Layer: " + key.toString());
            return null;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error getting Profile from MongoDB Layer: " + key.toString());
            return null;
        }
    }

    @Override
    public boolean has(UUID uuid) {
        try {
            return getQuery(uuid).find().toList().stream().findAny().isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error check if Profile exists in MongoDB Layer: " + uuid.toString());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Profile exists in MongoDB Layer: " + uuid.toString());
            return false;
        }
    }

    @Override
    public void remove(UUID key) {
        try {
            Query<X> q = getQuery(key);
            this.cache.getPayloadDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error removing Profile from MongoDB Layer: " + key.toString());
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error removing Profile from MongoDB Layer: " + key.toString());
        }
    }

    @Override
    public X get(ProfileData data) {
        return this.get(data.getUniqueId());
    }

    public X getByUsername(String username) {
        try {
            Query<X> q = getQueryForUsername(username);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            X x = xp.orElse(null);
            if (x != null) {
                x.interact();
                x.setLoadingSource(this.layerName());
            }
            return x;
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error getting Profile from MongoDB Layer: " + username);
            return null;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error getting Profile from MongoDB Layer: " + username);
            return null;
        }
    }

    @Override
    public boolean save(X payload) {
        payload.interact();
        try {
            this.cache.getPayloadDatabase().getDatastore().save(payload);
            return true;
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error saving Profile to MongoDB Layer: " + payload.getUsername());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error saving Profile to MongoDB Layer: " + payload.getUsername());
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
        int cleaned = 0;

        // Check for online players that don't have profiles
        for (Player player : this.cache.getPlugin().getServer().getOnlinePlayers()) {
            X payload = this.cache.getLocalProfile(player);
            if (payload == null) {

                // They don't have a profile but are online
                // Check if they are already failure handling
                ProfileData data = this.cache.getData(player.getUniqueId());
                if (data != null) {
                    if (this.cache.getFailureManager().hasFailure(data)) {
                        continue; // They are already being handled
                    }
                }

                if (data == null) {
                    data = this.cache.createData(player.getName(), player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
                }

                // They aren't failure handling if we got here, let them know of the issue and start failure handling
                player.sendMessage(this.cache.getLangController().get(PLang.PLAYER_ONLINE_NO_PROFILE));
                this.cache.getFailureManager().fail(data);
            }
        }

        return cleaned;
    }

    @Override
    public Collection<X> getAll() {
        return this.createQuery().find().toList();
    }

    @Override
    public long clear() {
        // For safety reasons...
        throw new UnsupportedOperationException("Cannot clear a MongoDB database from within Payload.");
    }

    @Override
    public long size() {
        return this.cache.getPayloadDatabase().getDatastore().find(this.cache.getPayloadClass()).count();
    }

    @Override
    public void init() {
        if (!this.cache.getPayloadDatabase().isStarted()) {
            this.cache.getErrorHandler().error(this.cache, "Error initializing MongoDB Profile Layer: Payload Database is not started");
        }
        if (this.cache.getSettings().isServerSpecific()) {
            this.addCriteriaModifier(query -> query.field("payloadId").equalIgnoreCase(PayloadAPI.get().getPayloadID()));
        }
    }

    @Override
    public void shutdown() {
        // Do nothing here.  MongoDB object closing will be handled when the cache shuts down.
    }

    @Override
    public String layerName() {
        return "Profile MongoDB";
    }

    public void addCriteriaModifier(PayloadQueryModifier<X> modifier) {
        this.queryModifiers.add(modifier);
    }

    public void removeCriteriaModifier(PayloadQueryModifier<X> modifier) {
        this.queryModifiers.remove(modifier);
    }

    public void applyQueryModifiers(Query<X> query) {
        for (PayloadQueryModifier<X> modifier : this.queryModifiers) {
            modifier.apply(query);
        }
    }

    public Query<X> createQuery() {
        Query<X> q = this.cache.getPayloadDatabase().getDatastore().createQuery(this.cache.getPayloadClass());
        this.applyQueryModifiers(q);
        return q;
    }

    public Query<X> getQuery(UUID uniqueId) {
        Query<X> q = this.createQuery();
        q.criteria("uniqueId").equalIgnoreCase(uniqueId.toString());
        return q;
    }

    public Query<X> getQueryForUsername(String username) {
        Query<X> q = this.createQuery();
        q.criteria("username").equalIgnoreCase(username);
        return q;
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

}
