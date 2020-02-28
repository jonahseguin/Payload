/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.base.Cache;

import javax.annotation.Nonnull;

public interface ObjectCache<X extends PayloadObject> extends Cache<String, X> {

    @Override
    @Nonnull
    PayloadObjectController<X> controller(@Nonnull String key);

}
