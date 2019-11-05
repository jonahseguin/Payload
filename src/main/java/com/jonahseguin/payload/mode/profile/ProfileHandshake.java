/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.jonahseguin.payload.base.handshake.Handshake;
import com.jonahseguin.payload.base.handshake.HandshakeData;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

public class ProfileHandshake<X extends PayloadProfile> extends Handshake {

    public static final String KEY_UUID = "uuid";
    private final ProfileCache<X> cache;
    private UUID uuid = null;

    @Inject
    public ProfileHandshake(@Nonnull ProfileCache<X> cache) {
        Preconditions.checkNotNull(cache);
        this.cache = cache;
    }

    public ProfileHandshake(@Nonnull ProfileCache<X> cache, @Nonnull UUID uuid) {
        this(cache);
        Preconditions.checkNotNull(uuid);
        this.uuid = uuid;
    }

    @Override
    public String channelPublish() {
        return "payload-profile-handshake-" + cache.getName();
    }

    @Override
    public String channelReply() {
        return "payload-profile-handshake-" + cache.getName() + "-reply";
    }

    @Override
    public void load(@Nonnull HandshakeData data) {
        this.uuid = UUID.fromString(data.getDocument().getString(KEY_UUID));
    }

    @Override
    public void write(@Nonnull HandshakeData data) {
        data.append(KEY_UUID, this.uuid);
    }

    @Override
    public void receive() {
        Optional<X> o = cache.getFromCache(uuid);
        if (o.isPresent()) {
            X profile = o.get();
            profile.setHandshakeStartTimestamp(System.currentTimeMillis());
            if (!cache.save(profile)) {
                cache.getErrorService().capture("Failed to save during handshake for " + profile.getName());
            }
        }
    }

    @Override
    public boolean shouldAccept() {
        Optional<X> o = cache.getLocalStore().get(uuid);
        return o.isPresent() && o.get().isOnline();
    }
}
