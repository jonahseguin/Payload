package com.jonahseguin.payload.profile.event;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.PayloadProfile;

import org.bukkit.event.Listener;

public class ProfileHaltedListener<T extends PayloadProfile> implements Listener {

    private final PayloadProfileCache<T> cache;

    public ProfileHaltedListener(PayloadProfileCache<T> cache) {
        this.cache = cache;
    }

    // TODO

}
