/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.base.failsafe.FailedPayload;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.base.type.PayloadController;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayloadObjectController<X extends PayloadObject> implements PayloadController<X> {

    private final ObjectCache<X> cache;
    private final ObjectData data;

    private boolean failure = false;
    private X payload = null;

    public PayloadObjectController(ObjectCache<X> cache, ObjectData data) {
        this.cache = cache;
        this.data = data;
    }

    @Override
    public X cache() {
        for (PayloadLayer<String, X, ObjectData> layer : this.cache.getLayerController().getLayers()) {
            try {
                if (layer.has(this.data)) {
                    this.cache.getErrorHandler().debug(this.cache, "Loading object " + this.data.getIdentifier() + " from layer " + layer.layerName());
                    payload = layer.get(this.data);
                    if (payload != null) {
                        break;
                    }
                }
            } catch (Exception ex) {
                failure = true;
                this.cache.getErrorHandler().exception(this.cache, ex, "Failed to load object " + this.data.getIdentifier() + " from layer " + layer.layerName());
            }
        }

        if (payload == null && this.failure) {
            // A failure occurred.. start failure handling
            if (!this.cache.getFailureManager().hasFailure(this.data)) {
                this.cache.getFailureManager().fail(this.data);
            }
            FailedPayload<X, ObjectData> failedPayload = this.cache.getFailureManager().getFailedPayload(data);
            if (failedPayload.getTemporaryPayload() == null) {
                failedPayload.setTemporaryPayload(this.cache.getInstantiator().instantiate(data));
            }
            return failedPayload.getTemporaryPayload();
        }

        if (payload != null) {
            this.cache.cache(payload);
            this.cache.getErrorHandler().debug(this.cache, "Cached payload " + payload.getIdentifier());
        }

        return payload;
    }
}
