package com.jonahseguin.payload.base.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CacheSettings {

    private final int failureRetryIntervalSeconds = 30;

}
