/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

public interface ProfileCache<X extends PayloadProfile> extends Cache<UUID, X, NetworkProfile> {

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

    @Override
    @Nonnull
    PayloadProfileController<X> controller(@Nonnull UUID key);

}
