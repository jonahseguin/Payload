/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.jonahseguin.payload.base.error.CacheErrorService;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.base.handshake.HandshakeModule;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.GuicePayloadInstantiator;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.base.type.PayloadInstantiator;

import javax.annotation.Nonnull;

public class CacheModule<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> extends AbstractModule {

    protected final PayloadCache<K, X, N, D> cache;

    public CacheModule(@Nonnull PayloadCache<K, X, N, D> cache) {
        Preconditions.checkNotNull(cache);
        this.cache = cache;
    }

    @Override
    protected void configure() {
        bind(new TypeLiteral<PayloadInstantiator<K, X, D>>() {
        }).to(new TypeLiteral<GuicePayloadInstantiator<K, X, N, D>>() {
        });
        install(new HandshakeModule());
        bind(ErrorService.class).to(new TypeLiteral<CacheErrorService<K, X, N, D>>() {
        }).in(Singleton.class);
    }

    @Provides
    PayloadCache<K, X, N, D> provideCache() {
        return this.cache;
    }

}
