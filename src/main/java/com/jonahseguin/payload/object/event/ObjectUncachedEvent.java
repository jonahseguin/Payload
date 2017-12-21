package com.jonahseguin.payload.object.event;

import com.jonahseguin.payload.object.obj.ObjectCacheable;
import lombok.Getter;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ObjectUncachedEvent<X extends ObjectCacheable> extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final X object;

    public ObjectUncachedEvent(X object) {
        this.object = object;
    }

    public ObjectUncachedEvent(boolean isAsync, X object) {
        super(isAsync);
        this.object = object;
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
}
