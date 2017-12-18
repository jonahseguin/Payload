package com.jonahseguin.payload.object.caching;

import com.jonahseguin.payload.object.obj.ObjectCacheable;
import lombok.Data;

@Data
public class ObjectLayerResult<X extends ObjectCacheable> {

    private final boolean success; // if it was able to provide
    private final boolean errors; // if any errors occurred
    private final X result;

}
