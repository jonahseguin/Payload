/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.google.inject.AbstractModule;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

public class CacheModule<K, X extends Payload<K>, D extends PayloadData> extends AbstractModule {

    @Override
    protected void configure() {
        bind(PayloadCacheService.class).to(PayloadDatabaseCacheService.class);
    }

}