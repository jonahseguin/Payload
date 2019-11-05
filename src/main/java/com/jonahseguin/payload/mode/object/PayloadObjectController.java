/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.failsafe.FailedPayload;
import com.jonahseguin.payload.base.type.PayloadController;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
@Setter
public class PayloadObjectController<X extends PayloadObject> implements PayloadController<X> {

    private final ObjectCache<X> cache;
    private final ObjectData data;

    private boolean failure = false;
    private X payload = null;

    PayloadObjectController(@Nonnull ObjectCache<X> cache, @Nonnull ObjectData data) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(data);
        this.cache = cache;
        this.data = data;
    }

    @Override
    public Optional<X> cache() {
        Optional<X> local = cache.getFromCache(data.getIdentifier());
        boolean fromLocal = false;
        if (local.isPresent()) {
            payload = local.get();
            fromLocal = true;
        } else {
            Optional<X> db = cache.getFromDatabase(data.getIdentifier());
            db.ifPresent(x -> payload = x);
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
            return Optional.ofNullable(failedPayload.getTemporaryPayload());
        }

        if (payload != null && !fromLocal) {
            this.cache.cache(payload);
            this.cache.getErrorService().debug("Cached payload " + payload.getIdentifier());
        }

        return Optional.ofNullable(payload);
    }

    @Override
    public void uncache(@Nonnull X payload, boolean switchingServers) {
        if (cache.isCached(payload.getIdentifier())) {
            cache.uncache(payload);
        }
        if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
            Optional<NetworkObject> o = cache.getNetworkService().get(payload.getIdentifier());
            if (o.isPresent()) {
                NetworkObject networkObject = o.get();
                networkObject.markUnloaded();
            }
        }
    }
}
