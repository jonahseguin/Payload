/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.service;

import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.base.type.Payload;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public interface PayloadService<K, X extends Payload<K>> extends Service {

    Optional<X> get(@Nonnull K key);

    Optional<X> getFromCache(@Nonnull K key);

    Optional<X> getFromDatabase(@Nonnull K key);

    boolean save(@Nonnull X payload);

    void saveAsync(@Nonnull X payload);

    void cache(@Nonnull X payload);

    void uncache(@Nonnull K key);

    void uncache(@Nonnull X payload);

    void delete(@Nonnull K key);

    void delete(@Nonnull X payload);

    boolean isCached(@Nonnull K key);

    void cacheAll();

    X create();

    int saveAll();

    Collection<X> all();

    Collection<X> cached();

}
