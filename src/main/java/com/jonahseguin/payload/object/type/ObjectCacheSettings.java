package com.jonahseguin.payload.object.type;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.common.util.DefaultPayloadSettings;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.obj.ObjectInstantiator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ObjectCacheSettings<X extends ObjectCacheable> {

    private final Class<X> type;
    private CacheDatabase cacheDatabase = new DefaultPayloadSettings.EmptyCacheDatabase();
    private CacheDebugger debugger = new DefaultPayloadSettings.EmptyDebugger();
    private boolean useRedis = true;
    private boolean useMongo = true;
    private boolean createOnNull = false; // if not found by ID in any layer: create (when true) == uses the ObjectInstantiator
    private ObjectInstantiator<X> objectInstantiator = null;
    private boolean saveAfterLoad = false; // whether to re-save the object to all layers except the one it was loaded from
    private String redisKey = "payload.object." + type.getSimpleName() + ".";
    private String mongoIdentifierField = "id"; // field name to query against identifier for object

}
