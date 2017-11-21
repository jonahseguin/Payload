package com.jonahseguin.payload.event;

import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import org.bukkit.event.Listener;

public class ProfileHaltedListener<T extends Profile> implements Listener {

    private final ProfileCache<T> cache;

    public ProfileHaltedListener(ProfileCache<T> cache) {
        this.cache = cache;
    }


}
