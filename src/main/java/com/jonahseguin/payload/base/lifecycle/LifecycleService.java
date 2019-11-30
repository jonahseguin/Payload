/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.lifecycle;

import com.jonahseguin.payload.base.Service;

public interface LifecycleService {

    boolean start(Service service);

    boolean shutdown(Service service);

}
