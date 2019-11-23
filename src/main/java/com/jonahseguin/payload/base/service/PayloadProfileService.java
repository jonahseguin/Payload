/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.service;

import com.jonahseguin.payload.mode.profile.NetworkProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;

public interface PayloadProfileService<X extends PayloadProfile> extends PayloadService<UUID, X, NetworkProfile> {

    Optional<X> get(@Nonnull String username);

    Optional<X> get(@Nonnull Player player);

    Future<Optional<X>> getAsync(@Nonnull String username);

    Future<Optional<X>> getAsync(@Nonnull Player player);

    Optional<X> getFromCache(@Nonnull String username);

    Optional<X> getFromCache(@Nonnull Player player);

    Optional<X> getFromDatabase(@Nonnull String username);

    Optional<X> getFromDatabase(@Nonnull Player player);

    ProfileCache<X> cache();

    boolean isCached(@Nonnull String username);

    boolean isCached(@Nonnull Player player);

    Collection<X> online();

}
