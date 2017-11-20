package com.jonahseguin.payload.event;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.type.CacheResult;
import lombok.Getter;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class AsyncCacheLoadFinishEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final ProfileCache<? extends Profile> profileCache;
    private final CacheResult cacheResult;

    public AsyncCacheLoadFinishEvent(ProfileCache<? extends Profile> profileCache, CacheResult cacheResult) {
        super(true);
        this.profileCache = profileCache;
        this.cacheResult = cacheResult;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

}
