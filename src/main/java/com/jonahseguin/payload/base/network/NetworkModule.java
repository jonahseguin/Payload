package com.jonahseguin.payload.base.network;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

public class NetworkModule<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<NetworkService<K, X, N, D>>() {
        })
                .to(new TypeLiteral<RedisNetworkService<K, X, N, D>>() {
                })
                .in(Singleton.class);
    }

}
