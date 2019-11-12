/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.inject.AbstractModule;
import com.jonahseguin.payload.annotation.Cache;
import com.jonahseguin.payload.base.error.CacheErrorService;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.base.handshake.HandshakeModule;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Getter;

@Getter
public class CacheModule<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> extends AbstractModule {

    protected final Class<K> keyType;
    protected final Class<X> payloadType;
    protected final Class<N> networkType;
    protected final Class<D> dataType;
    protected final String name;

    public CacheModule(Class<K> keyType, Class<X> payloadType, Class<N> networkType, Class<D> dataType, String name) {
        this.keyType = keyType;
        this.payloadType = payloadType;
        this.networkType = networkType;
        this.dataType = dataType;
        this.name = name;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Cache.class).toInstance(name);

        bind(ErrorService.class).to(CacheErrorService.class);
        install(new HandshakeModule());
    }
}
