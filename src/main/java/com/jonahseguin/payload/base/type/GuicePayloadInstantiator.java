package com.jonahseguin.payload.base.type;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.network.NetworkPayload;

public class GuicePayloadInstantiator<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> implements PayloadInstantiator<K, X, D> {

    private final PayloadCache<K, X, N, D> cache;
    private final Injector injector;

    @Inject
    public GuicePayloadInstantiator(PayloadCache<K, X, N, D> cache, Injector injector) {
        this.cache = cache;
        this.injector = injector;
    }

    @Override
    public X instantiate(D data) {
        return injector.getInstance(cache.getPayloadClass());
    }
}
