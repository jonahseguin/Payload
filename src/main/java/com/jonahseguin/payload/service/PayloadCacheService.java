/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.service;

import com.google.inject.Inject;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.plugin.Plugin;

public interface PayloadCacheService {

    <X extends PayloadProfile> ProfileCache<X> createProfileCache(String name, Class<X> type);

    <X extends PayloadObject> ObjectCache<X> createObjectCache(String name, Class<X> type);

}
