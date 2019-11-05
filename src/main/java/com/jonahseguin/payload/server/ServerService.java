/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.server;

import com.jonahseguin.payload.base.Service;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface ServerService extends Service {

    Optional<PayloadServer> get(@Nonnull String name);

    boolean has(@Nonnull String name);

    @Nonnull
    PayloadServer register(@Nonnull String name, boolean online);

    @Nonnull
    PayloadServer getThisServer();

    @Nonnull
    ServerPublisher getPublisher();

    Collection<PayloadServer> getServers();

}
