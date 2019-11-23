/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.handshake.HandshakeHandler;
import com.jonahseguin.payload.base.type.PayloadController;
import com.jonahseguin.payload.server.PayloadServer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class PayloadProfileController<X extends PayloadProfile> implements PayloadController<X> {

    private final PayloadProfileCache<X> cache;
    private final UUID uuid;
    private String username = null;
    private String loginIp = null;
    private boolean login = true; // whether this controller is being used during a login operation
    private boolean denyJoin = false;
    private String joinDenyReason = ChatColor.RED + "A caching error occurred.  Please try again.";
    private X payload = null;
    private Player player = null;
    private boolean failure = false;
    private boolean loadedFromLocal = false;

    PayloadProfileController(@Nonnull PayloadProfileCache<X> cache, @Nonnull UUID uuid) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(uuid);
        this.cache = cache;
        this.uuid = uuid;
    }

    public void reset() {
        denyJoin = false;
        payload = null;
        failure = false;
    }

    public void login(@Nonnull String username, @Nonnull String loginIp) {
        this.login = true;
        this.username = username;
        this.loginIp = loginIp;
    }
    
    @Override
    public Optional<X> cache() {
        reset();

        if (uuid != null && username != null) {
            // Map their UUID to Username
            cache.getUuidService().save(uuid, username);
        }

        if (login) {
            if (!cache.getDatabase().getState().canCacheFunction(cache)) {
                denyJoin = true;
                joinDenyReason = cache.getLang().module(cache).format("deny-join-database", cache.getName());
                return Optional.empty();
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
        if (cache.getLocalStore().has(uuid)) {
            return cache.getLocalStore().get(uuid);
        }
        Optional<X> o = cache.getMongoStore().get(uuid);

        if (!o.isPresent()) {
            // Failed to load from all layers

            // If there was a failure/error, start failure handling instead of making a new profile
            if (failure || !cache.getDatabase().getState().canCacheFunction(cache)) {
                denyJoin = true;
                joinDenyReason = ChatColor.RED + "The database is currently offline.  Please try again soon.";
                payload = null;
            } else if (login) {
                // Only make a new profile if they are logging in
                getCache().getErrorService().debug("Creating a new profile for Payload " + username);
                // Otherwise make a new profile
                payload = cache.getInstantiator().instantiate(cache.getInjector());
                if (username != null) {
                    payload.setUsername(username);
                }
                payload.setUUID(uuid);
                payload.setLoginIp(loginIp);
                payload.setLoadingSource("New Profile");
                cache.getPool().submit(() -> cache.save(payload));
            }
            // If they aren't logging in (getting a payload by UUID/username) and it wasn't found, return null as they don't exist.
        } else {
            payload = o.get();
            if (login) {
                // Update their login ip
                if (loginIp != null) {
                    payload.setLoginIp(loginIp);
                }
                if (username != null) {
                    payload.setUsername(username); // Update their username
                }
            }

            // Cache the Payload if successful
            cache.cache(payload);
        }

        return Optional.ofNullable(payload);
    }

    private void load(boolean local) {
        if (local) {
            Optional<X> o = cache.getLocalStore().get(uuid);
            if (o.isPresent()) {
                payload = o.get();
                loadedFromLocal = true;
                return;
            }
        }
        Optional<X> o = cache.getMongoStore().get(uuid);
        if (o.isPresent()) {
            payload = o.get();
        } else {
            if (login) {
                // Create
                payload = cache.getInstantiator().instantiate(cache.getInjector());
                if (username != null) {
                    payload.setUsername(username);
                }
                payload.setUUID(uuid);
                if (loginIp != null) {
                    payload.setLoginIp(loginIp);
                }
                payload.setLoadingSource("New Profile");
            } else {
                // Doesn't exist
                payload = null;
            }
        }

    }

    private Optional<X> cacheNetworkNode() {
        Player player = cache.getPlugin().getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            load(true);
        } else {
            cache.getErrorService().debug("Starting caching Payload [network-node] " + uuid.toString() + "(login: " + login + ")");
            Optional<NetworkProfile> oNP = cache.getNetworkService().get(uuid);
            NetworkProfile networkProfile = null;
            if (oNP.isPresent()) {
                networkProfile = oNP.get();
                if (networkProfile.isOnlineOtherServer()) {
                    PayloadServer server = cache.getServerService().get(networkProfile.getLastSeenServer()).orElse(null);
                    if (server != null && server.isOnline()) {
                        // Handshake
                        HandshakeHandler<ProfileHandshake> handshake = cache.getHandshakeService().publish(new ProfileHandshake(cache, uuid));
                        Optional<ProfileHandshake> o = handshake.waitForReply(cache.getSettings().getHandshakeTimeoutSeconds());
                        if (o.isPresent()) {
                            load(false);
                        } else {
                            // Timed out
                            denyJoin = true;
                            joinDenyReason = ChatColor.RED + "Timed out while loading your profile.  Please try again.";
                        }
                    } else {
                        // Target server isn't online
                        load(false);
                    }
                } else {
                    load(networkProfile.isOnlineThisServer()); // only load from local if they're online this server
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
        }

        if (payload != null && !loadedFromLocal) {
            if (username != null && !payload.getUsername().equalsIgnoreCase(username)) {
                cache.getErrorService().debug("Updated username: " + payload.getUsername() + " to " + username);
                payload.setUsername(username);
                if (!cache.save(payload)) {
                    cache.getErrorService().capture("Error saving Payload during caching after username update: " + payload.getUsername());
                }
            }
            if (login || cache.getSettings().isAlwaysCacheOnLoadNetworkNode()) {
                cache.cache(payload);
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
