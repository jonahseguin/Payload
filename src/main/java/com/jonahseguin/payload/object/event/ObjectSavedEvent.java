package com.jonahseguin.payload.object.event;

import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.type.OLayerType;
import lombok.Getter;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ObjectSavedEvent<X extends ObjectCacheable> extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final X object;
    private final OLayerType layer;
    private final PayloadObjectCache<X> objectCache;

    public ObjectSavedEvent(X object, OLayerType layer, PayloadObjectCache<X> objectCache) {
        this.object = object;
        this.layer = layer;
        this.objectCache = objectCache;
    }

    public ObjectSavedEvent(boolean isAsync, X object, OLayerType layer, PayloadObjectCache<X> objectCache) {
        super(isAsync);
        this.object = object;
        this.layer = layer;
        this.objectCache = objectCache;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public X getObject() {
        return object;
    }

    public OLayerType getLayer() {
        return layer;
    }

    public PayloadObjectCache<X> getObjectCache() {
        return objectCache;
    }
}
