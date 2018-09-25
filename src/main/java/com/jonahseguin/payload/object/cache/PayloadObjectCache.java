package com.jonahseguin.payload.object.cache;

import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.object.caching.ObjectCachingController;
import com.jonahseguin.payload.object.caching.ObjectLayerExecutorHandler;
import com.jonahseguin.payload.object.layers.ObjectLayerController;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.type.OLayerType;
import com.jonahseguin.payload.object.type.ObjectCacheSettings;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.plugin.Plugin;

@Getter
public class PayloadObjectCache<X extends ObjectCacheable> {

    private final String cacheID = UUID.randomUUID().toString();
    private final Plugin plugin;
    private final ObjectCacheSettings<X> settings;
    private final CacheDebugger debugger;
    private final ObjectLayerController<X> layerController;
    private final ObjectLayerExecutorHandler<X> executorHandler;
    private final Map<String, ObjectCachingController<X>> controllers = new HashMap<>();

    public PayloadObjectCache(Plugin plugin, ObjectCacheSettings<X> settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.debugger = settings.getDebugger();
        this.layerController = new ObjectLayerController<>(this);
        this.executorHandler = new ObjectLayerExecutorHandler<>(this);
    }

    public final boolean init() {
        boolean success = true;
        if (!layerController.init()) {
            success = false;
        }
        if (!success) {
            handleStartupFail();
        }
        return success;
    }

    public final boolean shutdown() {
        boolean success = true;
        if (!layerController.shutdown()) {
            success = false;
        }
        return success;
    }

    private void handleStartupFail() {
        debugger.error("[FATAL ERROR]  An error occurred while starting up the cache..");
        if(getDebugger().onStartupFailure()) {
            shutdown();
        }
    }

    public ObjectCachingController<X> getController(String id) {
        if (controllers.containsKey(id)) {
            return controllers.get(id);
        }
        ObjectCachingController<X> controller = new ObjectCachingController<>(this, id);
        controllers.put(id, controller);
        return controller;
    }

    public X cache(X x) {
        getLayerController().getLocalLayer().save(x.getIdentifier(), x);
        return x;
    }

    public X get(String id) {
        ObjectCachingController<X> controller = getController(id);
        return controller.cache();
    }

    public X getOrDefault(String id, X orElse) {
        X x = get(id);
        if (x != null) {
            return x;
        }
        else {
            return orElse;
        }
    }

    public X getFrom(OLayerType layerType, String id) {
        return getLayerController().getLayer(layerType).provide(id);
    }

    public X uncache(String id) {
        X x = getLayerController().getLocalLayer().provide(id);
        if (x != null) {
            getLayerController().getLocalLayer().remove(id);
        }
        return x;
    }

    public X uncache(X x) {
        return uncache(x.getIdentifier());
    }

    public void saveEverywhere(X x) {
        getLayerController().getLocalLayer().save(x.getIdentifier(), x);
        if (settings.isUseRedis()) {
            getLayerController().getRedisLayer().save(x.getIdentifier(), x);
        }
        if (settings.isUseMongo()) {
            getLayerController().getMongoLayer().save(x.getIdentifier(), x);
        }
    }

    public void deleteEverywhere(String id) {
        getLayerController().getLocalLayer().remove(id);
        if (settings.isUseRedis()) {
            getLayerController().getRedisLayer().remove(id);
        }
        if (settings.isUseMongo()) {
            getLayerController().getMongoLayer().remove(id);
        }
    }

}
