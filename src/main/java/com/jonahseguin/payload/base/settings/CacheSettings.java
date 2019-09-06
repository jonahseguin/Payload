package com.jonahseguin.payload.base.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CacheSettings {

    private int failureRetryIntervalSeconds = 30;
    private int autoSaveIntervalSeconds = 600; // 10 minutes

}
