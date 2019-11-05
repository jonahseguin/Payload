/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.base.handshake.Handshake;
import com.jonahseguin.payload.base.handshake.HandshakeData;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ObjectHandshake<X extends PayloadObject> extends Handshake {

    public static final String KEY_IDENTIFIER = "identifier";
    private final ObjectCache<X> cache;
    private String identifier = null;

    @Inject
    public ObjectHandshake(@Nonnull ObjectCache<X> cache) {
        Preconditions.checkNotNull(cache);
        this.cache = cache;
    }

    public ObjectHandshake(@Nonnull ObjectCache<X> cache, @Nonnull String identifier) {
        this(cache);
        Preconditions.checkNotNull(identifier);
        this.identifier = identifier;
    }

    @Override
    public String channelPublish() {
        return "payload-object-handshake-" + cache.getName();
    }

    @Override
    public String channelReply() {
        return "payload-object-handshake-" + cache.getName() + "-reply";
    }

    @Override
    public void load(@Nonnull HandshakeData data) {
        this.identifier = data.getDocument().getString(KEY_IDENTIFIER);
    }

    @Override
    public void write(@Nonnull HandshakeData data) {
        data.append(KEY_IDENTIFIER, identifier);
    }

    @Override
    public void receive() {
        Optional<X> o = cache.getFromCache(identifier);
        if (o.isPresent()) {
            X object = o.get();
            if (!cache.save(object)) {
                cache.getErrorService().capture("Failed to save during handshake for object " + object.getIdentifier());
            }
        }
    }

    @Override
    public boolean shouldAccept() {
        Optional<X> o = cache.getLocalStore().get(identifier);
        if (o.isPresent()) {
            X object = o.get();
            Optional<NetworkObject> ono = cache.getNetworked(object);
            if (ono.isPresent()) {
                NetworkObject no = ono.get();
                return no.isThisMostRelevantServer();
            }
        }
        return false;
    }
}
