package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.base.PayloadCacheService;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

public interface ProfileService<X extends PayloadProfile> extends PayloadCacheService<UUID, X, NetworkProfile, ProfileData> {

    Optional<X> get(@Nonnull String username);

    Optional<X> get(@Nonnull Player player);

    Future<Optional<X>> getAsync(@Nonnull String username);

    Future<Optional<X>> getAsync(@Nonnull Player player);

    Optional<X> getFromCache(@Nonnull String username);

    Optional<X> getFromCache(@Nonnull Player player);

    Optional<X> getFromDatabase(@Nonnull String username);

    Optional<X> getFromDatabase(@Nonnull Player player);

    @Nonnull
    Collection<X> getOnline();

    boolean isCached(@Nonnull String username);

    boolean isCached(@Nonnull Player player);

    @Nonnull
    ProfileCacheSettings getSettings();

}
