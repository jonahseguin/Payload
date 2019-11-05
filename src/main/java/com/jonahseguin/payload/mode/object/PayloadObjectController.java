/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.handshake.HandshakeHandler;
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

    private X payload = null;

    PayloadObjectController(@Nonnull ObjectCache<X> cache, @Nonnull ObjectData data) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(data);
        this.cache = cache;
        this.data = data;
    }

    private void load(boolean fromLocal) {
        if (fromLocal) {
            Optional<X> local = cache.getFromCache(data.getIdentifier());
            if (local.isPresent()) {
                payload = local.get();
                return;
            }
        }
        Optional<X> db = cache.getFromDatabase(data.getIdentifier());
        db.ifPresent(x -> payload = x);
    }

    @Override
    public Optional<X> cache() {
        if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
            Optional<NetworkObject> network = cache.getNetworked(data.getIdentifier());
            if (network.isPresent()) {
                NetworkObject no = network.get();
                if (no.isThisMostRelevantServer()) {
                    load(true);
                } else {
                    // Handshake
                    HandshakeHandler<ObjectHandshake<X>> h = cache.getHandshakeService().publish(new ObjectHandshake<>(cache, data.getIdentifier()));
                    h.waitForReply(cache.getSettings().getHandshakeTimeoutSeconds());
                    load(false);
                }

                if (payload != null) {
                    no.markLoaded();
                    cache.getNetworkService().save(no);
                }

            } else {
                // They have no network object, create it and load from the first available source
                load(true);
                if (payload != null) {
                    network = cache.getNetworked(payload);
                    if (network.isPresent()) {
                        NetworkObject no = network.get();
                        no.markLoaded();
                        cache.getNetworkService().save(no);
                    }
                }
            }
        } else {
            // Standalone mode
            load(true);
        }

        if (payload != null) {
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
