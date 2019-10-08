/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.settings;

import com.jonahseguin.payload.base.settings.CacheSettings;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileCacheSettings extends CacheSettings {

    private int localExpiryTimeSeconds = 7200;
    private int redisExpiryTimeSeconds = 14400;
    private int handshakeTimeoutSeconds = 5;
    private boolean denyJoinOnHandshakeTimeout = false; // will allow for failure handling
    private boolean denyJoinOnHandshakeFailDatabase = false; // deny join if database is down during handshake?
    private boolean denyJoinDatabaseDown = false;
    private int handshakeTimeOutAttemptsAllowJoin = 3;
    private boolean setOfflineOnShutdown = true; // update Profiles to offline=true on cache shutdown
    private boolean alwaysCacheOnLoadNetworkNode = false; // should we cache profiles that are fetched (not during login) in network_node mode (CAN CAUSE DATA LOSS)

}
