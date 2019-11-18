/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.type;


import com.google.inject.Injector;

import javax.annotation.Nonnull;

public interface PayloadInstantiator<K, X extends Payload<K>> {

    @Nonnull
    X instantiate(@Nonnull Injector injector);

}
