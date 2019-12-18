/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.handshake.Handshake;
import com.jonahseguin.payload.base.handshake.HandshakeData;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

@Getter
@Setter
public class SyncHandshake<K, X extends Payload<K>, N extends NetworkPayload<K>> extends Handshake {

    public static final String KEY_IDENTIFIER = "sync-identifier";
    public static final String KEY_MODE = "sync-mode";
    private final Cache<K, X, N> cache;
    private K identifier;
    private SyncHandshakeMode mode;

    public SyncHandshake(Injector injector, Cache<K, X, N> cache) {
        super(injector);
        this.cache = cache;
    }

    public SyncHandshake(Injector injector, Cache<K, X, N> cache, @Nonnull K identifier, @Nonnull SyncHandshakeMode mode) {
        super(injector);
        Preconditions.checkNotNull(identifier);
        this.cache = cache;
        this.identifier = identifier;
        this.mode = mode;
    }

    @Override
    public SyncHandshake<K, X, N> create() {
        return new SyncHandshake<>(injector, cache);
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
        if (mode.equals(SyncHandshakeMode.UNCACHE)) {
            if (cache.isCached(identifier)) {
                cache.uncache(identifier);
            }
        } else if (mode.equals(SyncHandshakeMode.UPDATE)) {
            if (cache.isCached(identifier) || cache.getSyncMode().equals(SyncMode.ALWAYS)) {
                cache.getFromDatabase(identifier).ifPresent(cache::cache);
            }
        }
    }

    @Override
    public boolean shouldAccept() {
        return cache.isCached(identifier) || cache.getSyncMode().equals(SyncMode.ALWAYS);
    }

    @Override
    public boolean shouldReply() {
        return mode.equals(SyncHandshakeMode.UPDATE); // only send a reply if its an update handshake.  uncache isn't a handshake
    }
}
