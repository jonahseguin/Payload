/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.base.CacheModule;

public class ObjectCacheModule<X extends PayloadObject> extends CacheModule<String, X, NetworkObject, ObjectData> {

    public ObjectCacheModule(Class<X> payloadType, String name) {
        super(String.class, payloadType, NetworkObject.class, ObjectData.class, name);
    }

    @Override
    protected void configure() {
        bind(keyType).to(String.class);
        bind(payloadType).to(payloadType);
        bind(networkType).to(NetworkObject.class);
        bind(dataType).to(ObjectData.class);
        super.configure();
    }

}
