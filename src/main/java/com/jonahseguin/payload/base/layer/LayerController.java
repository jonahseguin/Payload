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
public class LayerController<K, X extends Payload, D extends PayloadData> {

    private final ArrayList<PayloadLayer<K, X, D>> layers = new ArrayList<>();

    /**
     * Register a layer to be used within the caching process
     * @param layer the layer to register, in order
     */
    public void register(PayloadLayer<K, X, D> layer) {
        this.layers.add(layer);
    }

    public void init() {
        for (PayloadLayer<K, X, D> layer : this.layers) {
            layer.init();
        }
    }

    public void shutdown() {
        for (PayloadLayer<K, X, D> layer : this.layers) {
            layer.shutdown();
        }
    }

}
