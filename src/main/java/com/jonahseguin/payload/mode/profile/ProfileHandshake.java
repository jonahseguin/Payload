/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import com.jonahseguin.payload.base.handshake.Handshake;
import com.jonahseguin.payload.base.handshake.HandshakeData;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

public class ProfileHandshake extends Handshake {

    public static final String KEY_UUID = "uuid";
    public static final String KEY_SERVER = "targetServer";
    private final ProfileCache cache;
    private UUID uuid = null;
    private String targetServer = null;

    public ProfileHandshake(Injector injector, @Nonnull ProfileCache cache) {
        super(injector);
        Preconditions.checkNotNull(cache);
        this.cache = cache;
    }

    public ProfileHandshake(Injector injector, @Nonnull ProfileCache cache, @Nonnull UUID uuid, @Nonnull String targetServer) {
        this(injector, cache);
        Preconditions.checkNotNull(uuid);
        this.uuid = uuid;
        this.targetServer = targetServer;
    }

    @Override
    public ProfileHandshake create() {
        return new ProfileHandshake(injector, cache);
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
        try {
            this.uuid = UUID.fromString(data.getDocument().getString(KEY_UUID));
            this.targetServer = data.getDocument().getString(KEY_SERVER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void write(@Nonnull HandshakeData data) {
        try {
            data.append(KEY_UUID, this.uuid.toString());
            data.append(KEY_SERVER, this.targetServer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void receive() {
        Optional<PayloadProfile> o = cache.getFromCache(uuid);
        if (o.isPresent()) {
            PayloadProfile profile = o.get();
            profile.setHandshakeStartTimestamp(System.currentTimeMillis());
            if (!cache.save(profile)) {
                cache.getErrorService().capture("Failed to save during handshake for " + profile.getName());
            }
        } else {
            // We don't have them but they tried to handshake from here
            cache.getErrorService().debug("Skipping saving for Profile Handshake (not cached), replying: " + uuid.toString());
        }
    }

    @Override
    public boolean shouldReply() {
        return true;
    }

    @Override
    public boolean shouldAccept() {
        return targetServer.equalsIgnoreCase(cache.getDatabase().getServerService().getThisServer().getName());
    }
}
