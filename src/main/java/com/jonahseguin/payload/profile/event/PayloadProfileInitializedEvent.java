package com.jonahseguin.payload.profile.event;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.PayloadProfile;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Jonah on 12/12/2017.
 * Project: Payload
 *
 * @ 10:27 PM
 */
public class PayloadProfileInitializedEvent<X extends PayloadProfile> extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final X profile;
    private final PayloadProfileCache<X> cache;
    private final Player player;

    public PayloadProfileInitializedEvent(X profile, PayloadProfileCache<X> cache, Player player) {
        this.profile = profile;
        this.cache = cache;
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public X getProfile() {
        return profile;
    }

    public PayloadProfileCache<X> getCache() {
        return cache;
    }

    public Player getPlayer() {
        return player;
    }
}
