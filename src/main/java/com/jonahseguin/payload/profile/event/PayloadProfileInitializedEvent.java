package com.jonahseguin.payload.profile.event;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.Profile;
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
public class PayloadProfileInitializedEvent<X extends Profile> extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final X profile;
    private final PayloadProfileCache<X> cache;
    private final Player player;

    public PayloadProfileInitializedEvent(X profile, PayloadProfileCache<X> cache, Player player) {
        this.profile = profile;
        this.cache = cache;
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
