package com.jonahseguin.payload.profile.event;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.type.PCacheSource;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Jonah on 12/20/2017.
 * Project: Payload
 *
 * @ 6:10 PM
 */
public class PayloadProfilePreSaveEvent<X extends PayloadProfile> extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final X profile;
    private final PayloadProfileCache<X> profileCache;
    private final PCacheSource layer;

    public PayloadProfilePreSaveEvent(X profile, PayloadProfileCache<X> profileCache, PCacheSource layer) {
        this.profile = profile;
        this.profileCache = profileCache;
        this.layer = layer;
    }

    public PayloadProfilePreSaveEvent(boolean isAsync, X profile, PayloadProfileCache<X> profileCache, PCacheSource layer) {
        super(isAsync);
        this.profile = profile;
        this.profileCache = profileCache;
        this.layer = layer;
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

    public PayloadProfileCache<X> getProfileCache() {
        return profileCache;
    }

    public PCacheSource getLayer() {
        return layer;
    }
}
