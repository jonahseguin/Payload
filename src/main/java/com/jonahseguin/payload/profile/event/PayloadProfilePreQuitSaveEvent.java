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
public class PayloadProfilePreQuitSaveEvent<X extends PayloadProfile> extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final X profile;
    private final PayloadProfileCache<X> profileCache;

    public PayloadProfilePreQuitSaveEvent(X profile, PayloadProfileCache<X> profileCache) {
        this.profile = profile;
        this.profileCache = profileCache;
    }

    public PayloadProfilePreQuitSaveEvent(boolean isAsync, X profile, PayloadProfileCache<X> profileCache) {
        super(isAsync);
        this.profile = profile;
        this.profileCache = profileCache;
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

}
