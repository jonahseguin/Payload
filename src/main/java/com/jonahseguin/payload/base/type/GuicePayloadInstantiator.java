/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.type;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class GuicePayloadInstantiator<K, X extends Payload<K>> implements PayloadInstantiator<K, X> {

    private final Class<X> type;
    private final Injector injector;

    @Inject
    public GuicePayloadInstantiator(Class<X> type, Injector injector) {
        this.type = type;
        this.injector = injector;
    }

    @Override
    public X instantiate() {
        return injector.getInstance(type);
    }
}
