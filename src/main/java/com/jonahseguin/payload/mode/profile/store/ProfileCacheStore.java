/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.store;

import com.jonahseguin.payload.base.store.PayloadStore;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileCache;
import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class ProfileCacheStore<X extends PayloadProfile> implements PayloadStore<UUID, X> {

    protected final PayloadProfileCache<X> cache;

    public ProfileCacheStore(PayloadProfileCache<X> cache) {
        this.cache = cache;
    }
}
