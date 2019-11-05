/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object.settings;

import com.jonahseguin.payload.base.settings.CacheSettings;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObjectCacheSettings extends CacheSettings {

    private boolean useRedis = true;
    private boolean useMongo = true;
    private boolean createOnNull = false;
    private int handshakeTimeoutSeconds = 5;

}
