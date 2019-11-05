/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.jonahseguin.payload.mode.object.ObjectService;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileService;

import javax.annotation.Nonnull;

public interface CacheService extends Service {

    <X extends PayloadProfile> ProfileService<X> createProfileCache(@Nonnull String name, @Nonnull Class<X> type);

    <X extends PayloadObject> ObjectService<X> createObjectCache(@Nonnull String name, @Nonnull Class<X> type);

}
