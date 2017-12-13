package com.jonahseguin.payload.event;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import lombok.Getter;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Jonah on 12/12/2017.
 * Project: Payload
 *
 * @ 10:27 PM
 */
@Getter
public class PayloadPlayerInitializedEvent<X extends Profile> extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final X profile;
    private final ProfileCache<X> cache;
    private final Player player;

    public PayloadPlayerInitializedEvent(X profile, ProfileCache<X> cache, Player player) {
        this.profile = profile;
        this.cache = cache;
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
