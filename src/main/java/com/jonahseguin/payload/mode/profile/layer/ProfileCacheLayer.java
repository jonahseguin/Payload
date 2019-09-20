package com.jonahseguin.payload.mode.profile.layer;

import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class ProfileCacheLayer<X extends PayloadProfile> implements PayloadLayer<UUID, X, ProfileData> {

    protected final ProfileCache<X> cache;

    public ProfileCacheLayer(ProfileCache<X> cache) {
        this.cache = cache;
    }
}
