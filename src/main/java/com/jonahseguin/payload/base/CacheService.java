/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base;

import com.jonahseguin.payload.base.type.PayloadInstantiator;
import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.PayloadObject;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;

import javax.annotation.Nonnull;
import java.util.UUID;

public interface CacheService extends Service {

    <X extends PayloadProfile> ProfileCache<X> createProfileCache(@Nonnull String name, @Nonnull Class<X> type, @Nonnull PayloadInstantiator<UUID, X> instantiator);

    <X extends PayloadObject> ObjectCache<X> createObjectCache(@Nonnull String name, @Nonnull Class<X> type, @Nonnull PayloadInstantiator<String, X> instantiator);

}
