package com.jonahseguin.payload.base.layer;

import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Getter;

import java.util.ArrayList;

/**
 * The Layer Controller allows for registering "layers" in a cache internally.
 * Layers are what make up the actual caching internals
 * An example of a layer could be a Redis layer, followed by a MongoDB layer.
 * Every layer has the capability to provide the object being cached.  (and if it can't, the next one can)
 * @param <X> Payload
 * @param <D> PayloadData
 */
@Getter
public class LayerController<X extends Payload, D extends PayloadData> {

    private final ArrayList<PayloadLayer<X, D>> layers = new ArrayList<>();

    // want to allow layers to be registered by the cache
    // how can they set the order?
    //

    /**
     * Register a layer to be used within the caching process
     * @param layer the layer to register, in order
     */
    public void register(PayloadLayer<X, D> layer) {
        this.layers.add(layer);
    }

}
