/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.uuid;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jonahseguin.payload.PayloadPlugin;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class UUIDService {

    private final PayloadPlugin payloadPlugin;
    private final BiMap<UUID, String> cache = HashBiMap.create();

    @Inject
    public UUIDService(PayloadPlugin payloadPlugin) {
        this.payloadPlugin = payloadPlugin;
    }

    public void save(@Nonnull UUID uuid, @Nonnull String name) {
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(name);
        cache.put(uuid, name.toLowerCase());
    }

    public Optional<UUID> get(@Nonnull String name) {
        Preconditions.checkNotNull(name);
        return Optional.ofNullable(cache.inverse().get(name.toLowerCase()));
    }

    public Optional<String> get(@Nonnull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        return Optional.ofNullable(cache.get(uuid));
    }

    public boolean isCached(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public boolean isCached(String name) {
        return cache.inverse().containsKey(name);
    }

    public Optional<String> getNameFromOfflinePlayer(UUID uuid) {
        OfflinePlayer offlinePlayer = payloadPlugin.getServer().getOfflinePlayer(uuid);
        if (offlinePlayer != null) {
            return Optional.ofNullable(offlinePlayer.getName());
        }
        return Optional.empty();
    }

}
