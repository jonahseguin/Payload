package com.jonahseguin.payload.mode.profile.store;

import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class ProfileCacheStore<X extends PayloadProfile> implements PayloadStore<UUID, X, ProfileData> {

    protected final ProfileCache<X> cache;

    public ProfileCacheStore(ProfileCache<X> cache) {
        this.cache = cache;
    }
}
