/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object.layer;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.type.PayloadQueryModifier;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.ObjectData;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.mongodb.MongoException;
import dev.morphia.query.Query;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectLayerMongo<X extends PayloadObject> extends ObjectCacheLayer<X> {

    private final Set<PayloadQueryModifier<X>> queryModifiers = new HashSet<>();

    private X nullPayload = null; // for identifierFieldName

    public ObjectLayerMongo(ObjectCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(String key) {
        try {
            Query<X> q = getQuery(key);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            X x = xp.orElse(null);
            if (x != null) {
                x.interact();
            }
            return x;
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error getting Object from MongoDB Layer: " + key);
            return null;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error getting Object from MongoDB Layer: " + key);
            return null;
        }
    }

    @Override
    public boolean has(String key) {
        try {
            Query<X> q = getQuery(key);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error check if Object exists in MongoDB Layer: " + key);
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Object exists in MongoDB Layer: " + key);
            return false;
        }
    }

    @Override
    public void remove(String key) {
        try {
            Query<X> q = getQuery(key);
            this.cache.getPayloadDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error removing Object from MongoDB Layer: " + key);
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error removing Object from MongoDB Layer: " + key);
        }
    }

    @Override
    public X get(ObjectData data) {
        return this.get(data.getIdentifier());
    }

    @Override
    public boolean save(X payload) {
        payload.interact();
        try {
            this.cache.getPayloadDatabase().getDatastore().save(payload);
            return true;
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error saving Object to MongoDB Layer: " + payload.getIdentifier());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error saving Object to MongoDB Layer: " + payload.getIdentifier());
            return false;
        }
    }

    @Override
    public boolean has(ObjectData data) {
        try {
            Query<X> q = getQuery(data.getIdentifier());
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error check if Object exists in MongoDB Layer: " + data.getIdentifier());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Object exists in MongoDB Layer: " + data.getIdentifier());
            return false;
        }
    }

    @Override
    public boolean has(X payload) {
        payload.interact();
        try {
            Query<X> q = getQuery(payload.getIdentifier());
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error checking if Object exists in MongoDB Layer: " + payload.getIdentifier());
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Object exists in MongoDB Layer: " + payload.getIdentifier());
            return false;
        }
    }

    @Override
    public void remove(ObjectData data) {
        try {
            Query<X> q = getQuery(data.getIdentifier());
            this.cache.getPayloadDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error removing Object from MongoDB Layer: " + data.getIdentifier());
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error removing Object from MongoDB Layer: " + data.getIdentifier());
        }
    }

    @Override
    public void remove(X payload) {
        try {
            Query<X> q = getQuery(payload.getIdentifier());
            this.cache.getPayloadDatabase().getDatastore().findAndDelete(q);
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error removing Object from MongoDB Layer: " + payload.getIdentifier());
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error removing Object from MongoDB Layer: " + payload.getIdentifier());
        }
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
    public void init() {
        if (!this.cache.getPayloadDatabase().isStarted()) {
            this.cache.getErrorHandler().error(this.cache, "Error initializing MongoDB Object Layer: Payload Database is not started");
        }
        this.nullPayload = this.cache.getInstantiator().instantiate(new ObjectData(null));
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
        return "Object MongoDB";
    }

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
        Query<X> q = this.cache.getPayloadDatabase().getDatastore().createQuery(this.cache.getPayloadClass());
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
