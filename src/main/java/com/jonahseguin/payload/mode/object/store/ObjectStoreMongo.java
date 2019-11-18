/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object.store;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.base.type.PayloadQueryModifier;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.object.PayloadObjectCache;
import com.mongodb.MongoException;
import dev.morphia.query.Query;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectStoreMongo<X extends PayloadObject> extends ObjectCacheStore<X> {

    private final Set<PayloadQueryModifier<X>> queryModifiers = new HashSet<>();

    private boolean running = false;
    private X nullPayload = null; // for identifierFieldName

    public ObjectStoreMongo(PayloadObjectCache<X> cache) {
        super(cache);
    }

    @Override
    public Optional<X> get(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        try {
            Query<X> q = getQuery(key);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            xp.ifPresent(PayloadObject::interact);
            return xp;
        } catch (MongoException ex) {
            this.getCache().getErrorService().capture(ex, "MongoDB error getting Object from MongoDB Layer: " + key);
            return Optional.empty();
        } catch (Exception expected) {
            this.getCache().getErrorService().capture(expected, "Error getting Object from MongoDB Layer: " + key);
            return Optional.empty();
        }
    }

    @Override
    public boolean has(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        try {
            Query<X> q = getQuery(key);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorService().capture(ex, "MongoDB error check if Object exists in MongoDB Layer: " + key);
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorService().capture(expected, "Error checking if Object exists in MongoDB Layer: " + key);
            return false;
        }
    }

    @Override
    public void remove(@Nonnull String key) {
        Preconditions.checkNotNull(key);
        try {
            Query<X> q = getQuery(key);
            this.cache.getDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            this.getCache().getErrorService().capture(ex, "MongoDB error removing Object from MongoDB Layer: " + key);
        } catch (Exception expected) {
            this.getCache().getErrorService().capture(expected, "Error removing Object from MongoDB Layer: " + key);
        }
    }

    @Override
    public boolean save(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        payload.interact();
        try {
            return this.cache.getDatabase().getDatastore().save(payload) != null;
        } catch (MongoException ex) {
            this.getCache().getErrorService().capture(ex, "MongoDB error saving Object to MongoDB Layer: " + payload.getIdentifier());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorService().capture(expected, "Error saving Object to MongoDB Layer: " + payload.getIdentifier());
            return false;
        }
    }

    @Override
    public boolean has(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        payload.interact();
        try {
            Query<X> q = getQuery(payload.getIdentifier());
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorService().capture(ex, "MongoDB error checking if Object exists in MongoDB Layer: " + payload.getIdentifier());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorService().capture(expected, "Error checking if Object exists in MongoDB Layer: " + payload.getIdentifier());
            return false;
        }
    }

    @Override
    public void remove(@Nonnull X payload) {
        Preconditions.checkNotNull(payload);
        remove(payload.getIdentifier());
    }

    @Override
    public int cleanup() {
        return 0;
    }

    @Override
    public long clear() {
        // For safety reasons...
        throw new UnsupportedOperationException("Cannot clear a MongoDB database from within Payload.");
    }

    @Override
    public long size() {
        return this.createQuery().count();
    }

    @Override
    public boolean start() {
        running = true;
        if (!this.cache.getDatabase().isRunning()) {
            this.cache.getErrorService().capture("Error initializing MongoDB Object Layer: Payload Database is not started");
        }
        this.nullPayload = this.cache.getInstantiator().instantiate(cache.getInjector());
        Preconditions.checkNotNull(nullPayload, "Null payload failed to instantiate");
        if (this.cache.getSettings().isServerSpecific()) {
            this.addCriteriaModifier(query -> query.field("payloadId").equalIgnoreCase(cache.getApi().getPayloadID()));
        }
        return true;
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
        return "Object MongoDB";
    }

    @Nonnull
    @Override
    public Collection<X> getAll() {
        Query<X> q = this.createQuery();
        Stream<X> stream = q.find().toList().stream();
        return stream.collect(Collectors.toSet());
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
        Query<X> q = this.cache.getDatabase().getDatastore().createQuery(this.cache.getPayloadClass());
        this.applyQueryModifiers(q);
        return q;
    }

    public Query<X> getQuery(String key) {
        Query<X> q = createQuery();
        q.criteria(this.nullPayload.identifierFieldName()).equalIgnoreCase(key);
        return q;
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

}
