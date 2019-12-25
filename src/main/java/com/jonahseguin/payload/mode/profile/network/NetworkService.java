/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.network;

import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.mode.profile.PayloadProfile;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

public interface NetworkService<X extends PayloadProfile> extends Service {

    Optional<NetworkProfile> get(@Nonnull UUID key);

    Optional<NetworkProfile> get(@Nonnull X payload);

    boolean has(@Nonnull UUID key);

    boolean save(@Nonnull NetworkProfile networkProfile);

    Optional<X> get(@Nonnull NetworkProfile payload);

    NetworkProfile create(@Nonnull X payload);

}
