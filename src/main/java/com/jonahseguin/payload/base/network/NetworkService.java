/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.network;

import com.jonahseguin.payload.base.type.Payload;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface NetworkService<K, X extends Payload<K>, N extends NetworkPayload> {

    Optional<N> get(@Nonnull K key);

    boolean has(@Nonnull K key);

    void save(@Nonnull N payload);

    N save(@Nonnull X payload);

    Optional<X> get(@Nonnull N payload);

}
