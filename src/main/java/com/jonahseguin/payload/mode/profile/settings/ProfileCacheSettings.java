package com.jonahseguin.payload.mode.profile.settings;

import com.jonahseguin.payload.base.settings.CacheSettings;
import lombok.Data;

@Data
public class ProfileCacheSettings implements CacheSettings {

    private int localExpiryTimeSeconds = 7200;
    private int redisExpiryTimeSeconds = 14400;

}
