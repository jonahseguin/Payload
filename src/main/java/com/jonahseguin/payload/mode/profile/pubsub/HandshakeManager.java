package com.jonahseguin.payload.mode.profile.pubsub;

import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;

import java.util.UUID;

public class HandshakeManager<X extends PayloadProfile> {

    private final ProfileCache<X> cache;

    public HandshakeManager(ProfileCache<X> cache) {
        this.cache = cache;
    }

    public void emitServerFound(UUID uuid, String cacheName) {

    }

}
