/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.service;

import com.jonahseguin.payload.mode.object.ObjectCache;
import com.jonahseguin.payload.mode.object.PayloadObject;

public interface PayloadObjectService<X extends PayloadObject> extends PayloadService<String, X> {

    ObjectCache<X> cache();

}
