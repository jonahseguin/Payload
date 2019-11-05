/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.handshake.Handshake;
import com.jonahseguin.payload.base.handshake.HandshakeData;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
@Setter
public class SyncHandshake<K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> extends Handshake {

    public static final String KEY_IDENTIFIER = "sync-identifier";
    public static final String KEY_MODE = "sync-mode";
    private final PayloadCache<K, X, N, D> cache;
    private K identifier;
    private SyncHandshakeMode mode;

    @Inject
    public SyncHandshake(PayloadCache<K, X, N, D> cache) {
        this.cache = cache;
    }

    public SyncHandshake(PayloadCache<K, X, N, D> cache, @Nonnull K identifier, @Nonnull SyncHandshakeMode mode) {
        Preconditions.checkNotNull(identifier);
        this.cache = cache;
        this.identifier = identifier;
        this.mode = mode;
    }

    @Override
    public String channelPublish() {
        return "payload-sync-" + cache.getName();
    }

    @Override
    public String channelReply() {
        return "payload-sync-" + cache.getName() + "-reply";
    }

    @Override
    public void load(@Nonnull HandshakeData data) {
        identifier = cache.keyFromString(data.getDocument().getString(KEY_IDENTIFIER));
        mode = SyncHandshakeMode.valueOf(data.getDocument().getString(KEY_MODE));
    }

    @Override
    public void write(@Nonnull HandshakeData data) {
        data.append(KEY_IDENTIFIER, cache.keyToString(identifier));
        data.append(KEY_MODE, mode.name());
    }

    @Override
    public void receive() {
        Optional<X> o = cache.getFromCache(identifier);
        if (o.isPresent()) {
            X payload = o.get();
            if (!cache.saveNoSync(payload)) {
                cache.getErrorService().capture("Error saving payload during sync for " + cache.keyToString(payload.getIdentifier()));
            }
        }
    }

    @Override
    public boolean shouldAccept() {
        return cache.isCached(identifier);
    }

    @Override
    public boolean shouldReply() {
        return mode.equals(SyncHandshakeMode.UPDATE); // only send a reply if its an update handshake.  uncache isn't a handshake
    }
}
