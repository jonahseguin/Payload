package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.mode.object.layer.ObjectLayerLocal;
import com.jonahseguin.payload.mode.object.layer.ObjectLayerRedis;
import com.jonahseguin.payload.mode.object.settings.ObjectCacheSettings;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ObjectCache<X extends Payload> extends PayloadCache<String, X, ObjectData> {

    private transient final ObjectCacheSettings settings = new ObjectCacheSettings();
    private transient final ConcurrentMap<String, PayloadObjectController<X>> controllers = new ConcurrentHashMap<>();

    private transient final ConcurrentMap<String, ObjectData> data = new ConcurrentHashMap<>();

    // Layers
    private final ObjectLayerLocal<X> localLayer = new ObjectLayerLocal<>(this);
    private final ObjectLayerRedis<X> redisLayer = new ObjectLayerRedis<>(this);


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
        this.layerController.register(this.redisLayer);


        this.layerController.init();
    }

    @Override
    protected void shutdown() {

        this.layerController.shutdown();
    }

    @Override
    protected X get(String key) {
        return null;
    }

    @Override
    public long cachedObjectCount() {
        return this.localLayer.size();
    }

    @Override
    public boolean save(X payload) {
        return false;
    }

    @Override
    public void cache(X payload) {
        this.localLayer.save(payload);
    }

    @Override
    public int saveAll() {
        return 0;
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
        ObjectData data = new ObjectData(identifier);
        this.data.put(identifier, data);
        return data;
    }

}
