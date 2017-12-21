package com.jonahseguin.payload.object.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.event.ObjectPreSaveEvent;
import com.jonahseguin.payload.object.event.ObjectSavedEvent;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.type.OLayerType;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class OLocalLayer<X extends ObjectCacheable> extends ObjectCacheLayer<X> {

    private final Map<String, X> localCache = new ConcurrentHashMap<>();

    public OLocalLayer(PayloadObjectCache<X> cache, CacheDatabase database) {
        super(cache, database);
    }

    @Override
    public X provide(String id) {
        return localCache.get(id.toLowerCase());
    }

    @Override
    public boolean save(String id, X x) {

        // Pre-Save Event
        ObjectPreSaveEvent<X> preSaveEvent = new ObjectPreSaveEvent<>(x, source(), cache);
        getPlugin().getServer().getPluginManager().callEvent(preSaveEvent);
        x = preSaveEvent.getObject();

        localCache.put(id.toLowerCase(), x);

        // Saved Event
        ObjectSavedEvent<X> savedEvent = new ObjectSavedEvent<>(x, source(), cache);
        getPlugin().getServer().getPluginManager().callEvent(savedEvent);
        return true;
    }

    @Override
    public boolean has(String id) {
        return localCache.containsKey(id.toLowerCase());
    }

    @Override
    public boolean remove(String id) {
        localCache.remove(id.toLowerCase());
        return true;
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean shutdown() {
        localCache.clear();
        return true;
    }

    @Override
    public OLayerType source() {
        return OLayerType.LOCAL;
    }

    @Override
    public int cleanup() {
        return 0;
    }

    @Override
    public int clear() {
        int sizeBefore = localCache.size();
        localCache.clear();
        return sizeBefore;
    }
}
