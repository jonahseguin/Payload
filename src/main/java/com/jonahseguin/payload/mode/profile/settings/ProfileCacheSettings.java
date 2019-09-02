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

}
