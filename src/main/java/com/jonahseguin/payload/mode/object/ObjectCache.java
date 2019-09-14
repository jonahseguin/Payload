package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.mode.object.layer.ObjectLayerLocal;
import com.jonahseguin.payload.mode.object.layer.ObjectLayerMongo;
import com.jonahseguin.payload.mode.object.layer.ObjectLayerRedis;
import com.jonahseguin.payload.mode.object.settings.ObjectCacheSettings;
import lombok.Getter;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ObjectCache<X extends PayloadObject> extends PayloadCache<String, X, ObjectData> {

    private transient final ObjectCacheSettings settings = new ObjectCacheSettings();
    private transient final ConcurrentMap<String, PayloadObjectController<X>> controllers = new ConcurrentHashMap<>();

    private transient final ConcurrentMap<String, ObjectData> data = new ConcurrentHashMap<>();

    // Layers
    private final ObjectLayerLocal<X> localLayer = new ObjectLayerLocal<>(this);
    private final ObjectLayerRedis<X> redisLayer = new ObjectLayerRedis<>(this);
    private final ObjectLayerMongo<X> mongoLayer = new ObjectLayerMongo<>(this);


    public ObjectCache(PayloadHook hook, String name, Class<X> payloadClass) {
        super(hook, name, String.class, payloadClass);
    }

    @Override
    public PayloadObjectController<X> controller(ObjectData data) {
        if (controllers.containsKey(data.getIdentifier())) {
            return controllers.get(data.getIdentifier());
        }
        PayloadObjectController<X> controller = new PayloadObjectController<>(this, data);
        this.controllers.put(data.getIdentifier(), controller);
        return controller;
    }

    @Override
    public ObjectCacheSettings getSettings() {
        return this.settings;
    }

    @Override
    protected void init() {
        this.layerController.register(this.localLayer);
        if (this.settings.isUseRedis()) {
            this.layerController.register(this.redisLayer);
        }
        if (this.settings.isUseMongo()) {
            this.layerController.register(this.mongoLayer);
        }

        this.layerController.init();
    }

    @Override
    protected void shutdown() {
        this.data.clear();
        this.controllers.clear();
        this.layerController.shutdown();
    }

    public X getLocalObject(String key) {
        return this.localLayer.get(key);
    }

    public X getObject(String key) {
        return this.get(key);
    }

    public boolean hasObjectLocal(String key) {
        return this.localLayer.has(key);
    }

    public void removeObjectLocal(String key) {
        this.localLayer.remove(key);
    }

    public void removeObject(String key) {
        for (PayloadLayer<String, X, ObjectData> layer : this.layerController.getLayers()) {
            layer.remove(key);
        }
    }

    @Override
    protected X get(String key) {
        ObjectData data = this.createData(key);
        PayloadObjectController<X> controller = this.controller(data);
        return controller.cache();
    }

    @Override
    public long cachedObjectCount() {
        return this.localLayer.size();
    }

    @Override
    public boolean save(X payload) {
        boolean success = true;
        for (PayloadLayer<String, X, ObjectData> layer : this.layerController.getLayers()) {
            if (!layer.save(payload)) {
                success = false;
            }
        }
        return success;
    }

    @Override
    public void cache(X payload) {
        this.localLayer.save(payload);
    }

    @Override
    public int saveAll() {
        int failures = 0;
        for (X object : this.localLayer.getLocalCache().values()) {
            if (!this.save(object)) {
                failures++;
            }
        }
        return failures;
    }

    @Override
    public boolean requireRedis() {
        return this.settings.isUseRedis();
    }

    @Override
    public boolean requireMongoDb() {
        return this.settings.isUseMongo();
    }

    public ObjectData createData(String identifier) {
        if (this.data.containsKey(identifier)) {
            return this.data.get(identifier);
        }
        ObjectData data = new ObjectData(identifier);
        this.data.put(identifier, data);
        return data;
    }

    public ObjectData getData(String identifier) {
        return this.data.getOrDefault(identifier, null);
    }

    @Override
    public Collection<X> getCachedObjects() {
        return this.localLayer.getLocalCache().values();
    }

    @Override
    public void updatePayloadID() {

    }
}
