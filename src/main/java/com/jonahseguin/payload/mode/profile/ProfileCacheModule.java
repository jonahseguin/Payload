package com.jonahseguin.payload.mode.profile;

import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.jonahseguin.payload.base.CacheModule;
import com.jonahseguin.payload.base.network.NetworkModule;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ProfileCacheModule<X extends PayloadProfile> extends CacheModule<UUID, X, NetworkProfile, ProfileData> {

    private final ProfileCache<X> cache;

    ProfileCacheModule(@Nonnull ProfileCache<X> cache) {
        super(cache);
        this.cache = cache;
    }

    @Override
    protected void configure() {
        super.configure();
        bind(new TypeLiteral<Class<NetworkProfile>>() {
        }).toInstance(NetworkProfile.class);
        install(new NetworkModule<UUID, X, NetworkProfile, ProfileData>());
    }

    @Provides
    ProfileCache<X> provideProfileCache() {
        return this.cache;
    }

    @Provides
    ProfileService<X> provideProfileService() {
        return this.cache;
    }


}
