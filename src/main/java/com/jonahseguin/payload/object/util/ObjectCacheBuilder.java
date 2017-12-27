package com.jonahseguin.payload.object.util;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.obj.ObjectInstantiator;
import com.jonahseguin.payload.object.type.ObjectCacheSettings;

import org.bukkit.plugin.Plugin;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 7:24 PM
 */
public class ObjectCacheBuilder<T extends ObjectCacheable> {

    private final Plugin plugin;
    private final ObjectCacheSettings<T> settings;
    private final Class<T> objectClass;

    public ObjectCacheBuilder(Plugin plugin, Class<T> objectClass) {
        this.plugin = plugin;
        this.objectClass = objectClass;
        this.settings = new ObjectCacheSettings<>(objectClass);
    }

    public ObjectCacheBuilder<T> withDatabase(CacheDatabase cacheDatabase) {
        this.settings.setCacheDatabase(cacheDatabase);
        return this;
    }

    public ObjectCacheBuilder<T> withDebugger(CacheDebugger debugger) {
        this.settings.setDebugger(debugger);
        return this;
    }

    public ObjectCacheBuilder<T> withObjectInstantiator(ObjectInstantiator<T> instantiator) {
        this.settings.setObjectInstantiator(instantiator);
        return this;
    }

    public ObjectCacheBuilder<T> withRedis(boolean redis) {
        this.settings.setUseRedis(redis);
        return this;
    }

    public ObjectCacheBuilder<T> withMongo(boolean mongo) {
        this.settings.setUseMongo(mongo);
        return this;
    }

    public ObjectCacheBuilder<T> withCreateOnNull(boolean createOnNull) {
        this.settings.setCreateOnNull(createOnNull);
        return this;
    }

    public ObjectCacheBuilder<T> withSaveAfterLoad(boolean saveAfterLoad) {
        this.settings.setSaveAfterLoad(saveAfterLoad);
        return this;
    }

    public ObjectCacheBuilder<T> withRedisKey(String redisKey) {
        this.settings.setRedisKey(redisKey);
        return this;
    }

    public ObjectCacheBuilder<T> withMongoIdentifier(String mongoIdentifier) {
        this.settings.setMongoIdentifierField(mongoIdentifier);
        return this;
    }

    public PayloadObjectCache<T> build() {
        return new PayloadObjectCache<>(plugin, settings);
    }


}
