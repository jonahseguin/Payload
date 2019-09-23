package com.jonahseguin.payload.mode.object.layer;

import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectLayerMongo<X extends PayloadObject> extends ObjectCacheLayer<X> {

    private final Set<PayloadQueryModifier<X>> queryModifiers = new HashSet<>();

    private X nullPayload = null; // for identifierFieldName

    public ObjectLayerMongo(ObjectCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(String key) throws PayloadLayerCannotProvideException {
        if (!this.has(key)) {
            throw new PayloadLayerCannotProvideException("Cannot provide (does not have) in MongoDB layer for Object:" + key, this.cache);
        }
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
    public boolean has(String String) {
        try {
            Query<X> q = getQuery(String);
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error check if Object exists in MongoDB Layer: " + String);
            return false;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error checking if Object exists in MongoDB Layer: " + String);
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
    public X get(ObjectData data) throws PayloadLayerCannotProvideException {
        if (!this.has(data)) {
            throw new PayloadLayerCannotProvideException("Cannot provide (does not have) in MongoDB layer for Object:" + data.getIdentifier(), this.cache);
        }
        try {
            Query<X> q = getQuery(data.getIdentifier());
            Stream<X> stream = q.find().toList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.orElse(null);
        } catch (MongoException ex) {
            this.getCache().getErrorHandler().exception(this.getCache(), ex, "MongoDB error getting Object from MongoDB Layer: " + data.getIdentifier());
            return null;
        } catch (Exception expected) {
            this.getCache().getErrorHandler().exception(this.getCache(), expected, "Error getting Object from MongoDB Layer: " + data.getIdentifier());
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
        return this.cache.getPayloadDatabase().getDatastore().getCount(this.cache.getPayloadClass());
    }

    @Override
    public void init() {
        if (!this.cache.getPayloadDatabase().isStarted()) {
            this.cache.getErrorHandler().error(this.cache, "Error initializing MongoDB Object Layer: Payload Database is not started");
        }
        this.nullPayload = this.cache.getInstantiator().instantiate(new ObjectData(null));
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
        Stream<X> stream = q.asList().stream();
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
        q.maxTime(10, TimeUnit.SECONDS);
        q.criteria(this.nullPayload.identifierFieldName()).equalIgnoreCase(key);
        return q;
    }

    @Override
    public boolean isDatabase() {
        return true;
    }

}
