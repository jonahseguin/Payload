/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadCallback;
import com.jonahseguin.payload.base.handshake.HandshakeService;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

import javax.annotation.Nonnull;
import java.util.Optional;

public class CacheSyncService<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> implements SyncService<K, X, N, D> {

    private final PayloadCache<K, X, N, D> cache;
    private final HandshakeService handshakeService;
    private boolean running = false;

    @Inject
    public CacheSyncService(PayloadCache<K, X, N, D> cache, HandshakeService handshakeService) {
        this.cache = cache;
        this.handshakeService = handshakeService;
    }

    @Override
    public void prepareUpdate(@Nonnull X payload, @Nonnull PayloadCallback<Optional<X>> callback) {
        Preconditions.checkNotNull(payload);
        Preconditions.checkNotNull(callback);
        Optional<N> o = cache.getNetworked(payload);
        if (o.isPresent()) {
            N np = o.get();
            if (np.isThisMostRelevantServer()) {
                callback.callback(Optional.of(payload));
            } else {
                handshakeService.publish(new SyncHandshake<>(cache, payload.getIdentifier(), SyncHandshakeMode.UPDATE)).afterReply(h -> {
                    Optional<X> ox = cache.getFromDatabase(payload.getIdentifier());
                    callback.callback(ox);
                });
            }
        } else {
            callback.callback(Optional.empty());
        }
    }

    @Override
    public void update(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        handshakeService.publish(new SyncHandshake<>(cache, key, SyncHandshakeMode.UPDATE)).afterReply(h -> {
            cache.controller(key).cache();
        });
    }

    @Override
    public void uncache(@Nonnull K key) {
        Preconditions.checkNotNull(key);
        handshakeService.publish(new SyncHandshake<>(cache, key, SyncHandshakeMode.UNCACHE));
    }

    @Override
    public boolean start() {
        running = true;
        handshakeService.subscribe(Key.get(new TypeLiteral<SyncHandshake<K, X, N, D>>() {
        }));
        return true;
    }

    @Override
    public boolean shutdown() {
        running = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
