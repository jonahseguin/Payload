package com.jonahseguin.payload.event;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.Profile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 8:07 PM
 */
@Getter
public class PayloadPlayerLoadedEvent<X extends Profile> extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final CachingProfile<X> cachingProfile;
    private final ProfileCache<X> cache;
    private final X profile;

    public PayloadPlayerLoadedEvent(CachingProfile<X> cachingProfile, ProfileCache<X> cache, X profile) {
        super(true);
        this.cachingProfile = cachingProfile;
        this.cache = cache;
        this.profile = profile;
    }

    public Player tryToGetPlayer() {
        return Bukkit.getPlayerExact(profile.getName());
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
