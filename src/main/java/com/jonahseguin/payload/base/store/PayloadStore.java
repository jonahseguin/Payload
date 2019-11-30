/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.store;

import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.base.type.Payload;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface PayloadStore<K, X extends Payload> extends Service {

    Optional<X> get(@Nonnull K key);

    boolean save(@Nonnull X payload);

    boolean has(@Nonnull K key);

    boolean has(@Nonnull X payload);

    void remove(@Nonnull K key);

    void remove(@Nonnull X payload);

    @Nonnull
    Collection<X> getAll();

    int cleanup();

    long clear();

    @Nonnull
    String layerName();

    long size();

    boolean isDatabase();

}
