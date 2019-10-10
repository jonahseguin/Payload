/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

public enum SyncMode {

    UPDATE, // We will only update locally stored objects with matching IDs

    CACHE_ALL // We will cache all objects we receive updates for

}
