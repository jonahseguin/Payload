/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.service;

import com.jonahseguin.payload.base.PayloadCallback;
import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;

public interface PayloadService<K, X extends Payload<K>, N extends NetworkPayload<K>> extends Service {

    Optional<N> getNetworked(@Nonnull K key);

    Optional<N> getNetworked(@Nonnull X payload);

    Optional<X> get(@Nonnull K key);

    Future<Optional<X>> getAsync(@Nonnull K key);

    Optional<X> getFromCache(@Nonnull K key);

    Optional<X> getFromDatabase(@Nonnull K key);

    boolean save(@Nonnull X payload);

    Future<Boolean> saveAsync(@Nonnull X payload);

    boolean saveNoSync(@Nonnull X payload);

    void cache(@Nonnull X payload);

    void uncache(@Nonnull K key);

    void uncache(@Nonnull X payload);

    void delete(@Nonnull K key);

    void delete(@Nonnull X payload);

    boolean isCached(@Nonnull K key);

    void prepareUpdate(@Nonnull X payload, @Nonnull PayloadCallback<Optional<X>> callback);

    void prepareUpdateAsync(@Nonnull X payload, @Nonnull PayloadCallback<Optional<X>> callback);

    void cacheAll();

    X create();

    N createNetworked();

    int saveAll();

    Collection<X> all();

    Collection<X> cached();

}
