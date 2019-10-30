/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;

public interface CacheService {

    <X extends PayloadProfile> ProfileCache<X> createProfileCache(String name, Class<X> type);

    <X extends PayloadObject> ObjectCache<X> createObjectCache(String name, Class<X> type);

    <X extends PayloadProfile> ProfileCache<X> getProfileCache(String name);

    <X extends PayloadObject> ObjectCache<X> getObjectCache(String name);

}
