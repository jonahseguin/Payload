/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.settings;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class CacheSettings {

    private int failureRetryIntervalSeconds = 30;
    private int autoSaveIntervalSeconds = 600; // 10 minutes
    private int cleanupIntervalSeconds = 1200;
    private boolean serverSpecific = false; // should we associate each object with a server, and only cache objects that match this server
    private boolean enableSync = true; // Enable the payload sync service.  This will sync objects/profiles (Payloads) across multiple servers, in a policy specific to the SyncMode
}
