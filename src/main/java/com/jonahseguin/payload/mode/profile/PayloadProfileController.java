/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.handshake.HandshakeHandler;
import com.jonahseguin.payload.base.type.PayloadController;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Optional;

@Getter
@Setter
public class PayloadProfileController<X extends PayloadProfile> implements PayloadController<X> {

    private final ProfileCache<X> cache;
    private final ProfileData data;
    private boolean login = true; // whether this controller is being used during a login operation
    private boolean denyJoin = false;
    private String joinDenyReason = ChatColor.RED + "A caching error occurred.  Please try again.";
    private X payload = null;
    private Player player = null;
    private boolean failure = false;

    PayloadProfileController(@Nonnull ProfileCache<X> cache, @Nonnull ProfileData data) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(data);
        this.cache = cache;
        this.data = data;
    }

    public void reset() {
        denyJoin = false;
        payload = null;
        failure = false;
    }

    @Override
    public Optional<X> cache() {
        reset();

        if (data.getUniqueId() != null && data.getUsername() != null) {
            // Map their UUID to Username
            cache.getUuidService().save(data.getUniqueId(), data.getUsername());
        }

        if (login) {
            if (cache.getSettings().isDenyJoinDatabaseDown()) {
                if (!cache.getDatabase().getState().canCacheFunction(cache)) {
                    denyJoin = true;
                    joinDenyReason = cache.getLang().module(cache).format("deny-join-database", cache.getName());
                    return Optional.empty();
                }
            }
        }

        if (cache.getMode().equals(PayloadMode.STANDALONE)) {
            return cacheStandalone();
        } else if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
            return cacheNetworkNode();
        } else {
            throw new UnsupportedOperationException("Unknown cache mode: " + cache.getMode().toString());
        }
    }

    @Override
    public void uncache(@Nonnull X payload, boolean switchingServers) {
        if (cache.isCached(payload.getUniqueId())) {
            cache.uncache(payload.getUniqueId());
        }
        if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
            Optional<NetworkProfile> o = cache.getNetworkService().get(payload.getUniqueId());
            if (o.isPresent()) {
                NetworkProfile networkProfile = o.get();
                networkProfile.markUnloaded(switchingServers);
            }
        }
    }

    private Optional<X> cacheStandalone() {
        // Iterate each layer in order
        if (cache.getLocalStore().has(data.getUniqueId())) {
            return cache.getLocalStore().get(data.getUniqueId());
        }
        Optional<X> o = cache.getMongoStore().get(data.getUniqueId());

        if (!o.isPresent()) {
            // Failed to load from all layers

            // If there was a failure/error, start failure handling instead of making a new profile
            if (failure || !cache.getDatabase().getState().canCacheFunction(cache)) {
                // start failure handling
                if (cache.getSettings().isDenyJoinDatabaseDown()) {
                    denyJoin = true;
                    joinDenyReason = "&cThe database is currently offline.  Please try again soon.";
                } else {
                    if (!cache.getFailureManager().hasFailure(data)) {
                        cache.getFailureManager().fail(data);
                    }
                }
            } else if (login) {
                // Only make a new profile if they are logging in
                getCache().getErrorService().debug("Creating a new profile for Payload " + data.getUsername());
                // Otherwise make a new profile
                payload = cache.getInstantiator().instantiate(data);
                payload.setLoadingSource("New Profile");
                cache.getPool().submit(() -> cache.save(payload));
            }
            // If they aren't logging in (getting a payload by UUID/username) and it wasn't found, return null as they don't exist.
        } else {
            payload = o.get();
            if (login) {
                // Update their login ip
                if (data.getIp() != null) {
                    payload.setLoginIp(data.getIp());
                }
                if (data.getUsername() != null) {
                    payload.setUsername(data.getUsername()); // Update their username
                }
            }

            // Cache the Payload if successful
            cache.cache(payload);
        }

        return Optional.ofNullable(payload);
    }

    private void load(boolean local) {
        if (local) {
            Optional<X> o = cache.getLocalStore().get(data.getUniqueId());
            if (o.isPresent()) {
                payload = o.get();
                return;
            }
        }
        Optional<X> o = cache.getMongoStore().get(data.getUniqueId());
        if (o.isPresent()) {
            payload = o.get();
        } else {
            if (login) {
                // Create
                payload = cache.getInstantiator().instantiate(data);
                payload.setLoadingSource("New Profile");
            } else {
                // Doesn't exist
                payload = null;
            }
        }

    }

    private Optional<X> cacheNetworkNode() {
        cache.getErrorService().debug("Starting caching Payload [network-node] " + getData().getIdentifier() + "(login: " + login + ")");
        Optional<NetworkProfile> oNP = cache.getNetworkService().get(data.getUniqueId());
        NetworkProfile networkProfile = null;
        if (oNP.isPresent()) {
            networkProfile = oNP.get();
            if (networkProfile.isOnline() && !networkProfile.getLastSeenServer().getName().equalsIgnoreCase(cache.getServerService().getThisServer().getName())) {
                if (networkProfile.getLastSeenServer().isOnline()) {
                    // Handshake
                    HandshakeHandler<ProfileHandshake<X>> handshake = cache.getHandshakeService().publish(new ProfileHandshake<>(cache));
                    Optional<ProfileHandshake<X>> o = handshake.waitForReply(10);
                    if (o.isPresent()) {
                        load(false);
                    } else {
                        // Timed out
                        denyJoin = true;
                        joinDenyReason = "&cTimed out while loading your profile.  Please try again.";
                    }
                } else {
                    // Target server isn't online
                    load(false);
                }
            } else {
                load(true);
            }
        } else {
            // Create the network profile
            load(true);
            if (payload != null) {
                if (login) {
                    networkProfile = cache.getNetworkService().create(payload);
                }
            }
        }

        if (networkProfile != null) {
            networkProfile.markLoaded(login);
            NetworkProfile finalNetworkProfile = networkProfile;
            cache.runAsync(() -> cache.getNetworkService().save(finalNetworkProfile));
        }
        if (payload != null) {
            if (!payload.getUsername().equalsIgnoreCase(data.getUsername())) {
                cache.getErrorService().debug("Updated username: " + payload.getUsername() + " to " + data.getUsername());
                payload.setUsername(data.getUsername());
                if (!cache.save(payload)) {
                    cache.getErrorService().capture("Error saving Payload during caching after username update: " + payload.getUsername());
                }
                if (login || cache.getSettings().isAlwaysCacheOnLoadNetworkNode()) {
                    cache.cache(payload);
                }
            }
        }
        return Optional.ofNullable(payload);
    }

    public void initializeOnJoin(Player player) {
        this.player = player;
        if (payload != null) {
            payload.initializePlayer(player);
        }
    }

}
