package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadController;

public class PayloadObjectController<X extends Payload> implements PayloadController<X> {

    private final ObjectCache<X> cache;
    private final ObjectData data;

    private X payload = null;

    public PayloadObjectController(ObjectCache<X> cache, ObjectData data) {
        this.cache = cache;
        this.data = data;
    }

    @Override
    public X cache() {
        return null;
    }
}
