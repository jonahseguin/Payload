package com.jonahseguin.payload.event;

import com.jonahseguin.payload.profile.CachingProfile;
import lombok.Getter;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class AsyncCacheLoadStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final CachingProfile cachingProfile;

    public AsyncCacheLoadStartEvent(CachingProfile cachingProfile) {
        super(true);
        this.cachingProfile = cachingProfile;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
