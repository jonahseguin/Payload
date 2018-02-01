package com.jonahseguin.payload.pabstract.layer;

import com.jonahseguin.payload.pabstract.type.AbstractCacheable;

/**
 * Created by Jonah on 1/26/2018.
 * Project: Payload
 *
 * @ 8:29 PM
 */
public abstract class AbstractLayer<T extends AbstractCacheable> {

    public abstract T provide(String key);

    public abstract boolean has(String key);

    public abstract boolean remove(String key);

}
