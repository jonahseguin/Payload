package com.jonahseguin.payload.object.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.event.ObjectPreSaveEvent;
import com.jonahseguin.payload.object.event.ObjectSavedEvent;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.type.OLayerType;
import com.mongodb.MongoException;
import org.mongodb.morphia.query.Query;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OMongoLayer<X extends ObjectCacheable> extends ObjectCacheLayer<X> {

    private final Class<X> clazz;

    public OMongoLayer(PayloadObjectCache<X> cache, CacheDatabase database, Class<X> clazz) {
        super(cache, database);
        this.clazz = clazz;
    }

    @Override
    public X provide(String id) {
        if (!cache.getSettings().isUseMongo()) return null;
        try {
            Query<X> q = getQuery(id);
            Stream<X> stream = q.asList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.orElse(null);
        } catch (Exception ex) {
            error(ex, "Exception occurred while trying to provide for Object from MongoDB for ID: " + id);
            return null;
        }
    }

    @Override
    public boolean save(String id, X x) {
        if (!cache.getSettings().isUseMongo()) return false;
        if (!x.persist()) return false;
        try {
            // Pre-Save Event
            ObjectPreSaveEvent<X> preSaveEvent = new ObjectPreSaveEvent<>(x, source(), cache);
            getPlugin().getServer().getPluginManager().callEvent(preSaveEvent);
            x = preSaveEvent.getObject();

            database.getDatastore().save(x);

            // Saved Event
            ObjectSavedEvent<X> savedEvent = new ObjectSavedEvent<>(x, source(), cache);
            getPlugin().getServer().getPluginManager().callEvent(savedEvent);

            return true;
        } catch (MongoException ex) {
            error(ex, "A MongoDB exception occurred while trying to save object by ID: " + id);
        } catch (Exception ex) {
            error(ex, "Could not save object to MongoDB by ID: " + id);
        }
        return false;
    }

    @Override
    public boolean has(String id) {
        if (!cache.getSettings().isUseMongo()) return false;
        try {
            Query<X> q = getQuery(id);
            Stream<X> stream = q.asList().stream();
            Optional<X> xp = stream.findFirst();
            return xp.isPresent();
        } catch (MongoException ex) {
            error(ex, "A MongoDB exception occurred while checking if object is in MongoDB");
        } catch (Exception ex) {
            error(ex, "Could not check if object is in MongoDB");
        }
        return false;
    }

    @Override
    public boolean remove(String id) {
        if (!cache.getSettings().isUseMongo()) return false;
        try {
            Query<X> q = getQuery(id);
            X obj = getDatabase().getDatastore().findAndDelete(q);
            return obj != null;
        } catch (MongoException ex) {
            error(ex, "A MongoDB exception occurred while deleting object from MongoDB");
        } catch (Exception ex) {
            error(ex, "Could not remove (delete) object from MongoDB");
        }
        return false;
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean shutdown() {
        if (!cache.getSettings().isUseMongo()) return false;
        getCache().getLayerController().getLocalLayer().getLocalCache().values().forEach(x -> {
            if (x.persist()) {
                save(x.getIdentifier(), x);
            }
        });
        return false;
    }

    public Set<X> getAll() {
        Query<X> q = database.getDatastore().createQuery(clazz);
        return q.asList().stream().collect(Collectors.toSet());
    }

    @Override
    public OLayerType source() {
        return OLayerType.MONGO;
    }

    @Override
    public int cleanup() {
        return 0;
    }

    @Override
    public int clear() {
        return 0;
    }

    public Query<X> getQuery(String id) {
        Query<X> q = database.getDatastore().createQuery(clazz);
        q.maxTime(10, TimeUnit.SECONDS);
        q.criteria(getCache().getSettings().getMongoIdentifierField()).equalIgnoreCase(id);
        return q;
    }

}
