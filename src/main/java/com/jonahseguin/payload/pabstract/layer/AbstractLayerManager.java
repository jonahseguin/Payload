package com.jonahseguin.payload.pabstract.layer;

import com.jonahseguin.payload.pabstract.type.AbstractCacheable;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Jonah on 1/26/2018.
 * Project: Payload
 *
 * @ 8:30 PM
 */
@Getter
public class AbstractLayerManager<T extends AbstractCacheable> {

    private final ConcurrentMap<Integer, AbstractLayer<T>> layers = new ConcurrentHashMap<>();

    public void inject(int priority, AbstractLayer<T> layer) {
        layers.put(priority, layer);
    }

}
