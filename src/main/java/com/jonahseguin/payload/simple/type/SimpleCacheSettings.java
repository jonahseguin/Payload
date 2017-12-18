package com.jonahseguin.payload.simple.type;

import com.jonahseguin.payload.simple.simple.PlayerCacheable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Created by Jonah on 12/17/2017.
 * Project: Payload
 *
 * @ 5:40 PM
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SimpleCacheSettings<X extends PlayerCacheable> {

    private final SimpleInstantiator<X> instantiator;
    private boolean removeOnLogout = false;
    private int expiryMinutesAfterLogout = 30;
    private int cacheCleanupCheckIntervalMinutes = 2;

}
