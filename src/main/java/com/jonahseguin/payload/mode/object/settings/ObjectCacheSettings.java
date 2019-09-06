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
    private int redisExpiryTimeSeconds = -1; // -1 for no expiry

}
