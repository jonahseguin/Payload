package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.base.PayloadCacheService;

public interface ObjectService<X extends PayloadObject> extends PayloadCacheService<String, X, NetworkObject, ObjectData> {


}
