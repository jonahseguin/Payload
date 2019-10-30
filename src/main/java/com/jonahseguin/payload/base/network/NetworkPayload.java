/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.network;

import com.jonahseguin.payload.server.PayloadServer;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.Set;

public abstract class NetworkPayload {

    protected ObjectId objectId;
    protected Date lastCached;
    protected PayloadServer lastSeenServer;
    protected Date lastSaved;
    protected boolean loaded;
    protected Set<PayloadServer> loadedServers;

}
