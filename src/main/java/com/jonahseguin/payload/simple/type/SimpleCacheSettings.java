package com.jonahseguin.payload.simple.type;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.common.util.DefaultPayloadSettings;
import com.jonahseguin.payload.simple.obj.SimpleCacheable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SimpleCacheSettings<X extends SimpleCacheable> {

    private final SimpleCacheType cacheType;
    private final Class<X> type;
    private CacheDatabase cacheDatabase = new DefaultPayloadSettings.EmptyCacheDatabase();
    private CacheDebugger debugger = new DefaultPayloadSettings.EmptyDebugger();
    private boolean useLocal = true;
    private boolean useRedis = true;
    private boolean useMongo = true;
    private boolean expireLocal = true;
    private int expiryLocalMinutes = 30; // for players after logout


}
