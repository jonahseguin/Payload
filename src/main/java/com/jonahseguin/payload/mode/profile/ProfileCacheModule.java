/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.base.CacheModule;

import java.util.UUID;

public class ProfileCacheModule<X extends PayloadProfile> extends CacheModule<UUID, X, NetworkProfile, ProfileData> {

    public ProfileCacheModule(Class<X> payloadType, String name) {
        super(UUID.class, payloadType, NetworkProfile.class, ProfileData.class, name);
    }

    @Override
    protected void configure() {
        bind(payloadType).to(payloadType);
        bind(networkType).to(NetworkProfile.class);
        bind(dataType).to(ProfileData.class);
        super.configure();
    }



}
