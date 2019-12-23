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
    private boolean login = false; // whether this controller is being used during a login operation
    private boolean denyJoin = false;
    private String joinDenyReason = ChatColor.RED + "A caching error occurred.  Please try again.";
    private X payload = null;
    private Player player = null;
    private boolean failure = false;
    private int timeoutAttempts = 0;

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
        if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
            if (cache.isCached(payload.getUniqueId())) {
                cache.uncache(payload.getUniqueId());
            }
        }
        Optional<NetworkProfile> o = cache.getNetworkService().get(payload.getUniqueId());
        if (o.isPresent()) {
            NetworkProfile networkProfile = o.get();
            networkProfile.markUnloaded(switchingServers);
            cache.runAsync(() -> cache.getNetworkService().save(networkProfile));
        }
    }

    private Optional<X> cacheStandalone() {
        // Iterate each layer in order
        Optional<X> localO = cache.getLocalStore().get(uuid);
        if (localO.isPresent()) {
            payload = localO.get();
        }
        else {
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
            }
            if (payload != null) {
                if (login) {
                    timeoutAttempts = 0;

                    Optional<NetworkProfile> oNP = cache.getNetworked(payload);
                    NetworkProfile networkProfile = oNP.orElseGet(() -> cache.getNetworkService().create(payload));

                    if (networkProfile != null) {
                        networkProfile.markLoaded(login);
                        cache.runAsync(() -> cache.getNetworkService().save(networkProfile));
                    }

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
        }

        return Optional.ofNullable(payload);
    }

    private void load(boolean local) {
        if (local) {
            Optional<X> o = cache.getLocalStore().get(uuid);
            if (o.isPresent()) {
                payload = o.get();
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
        if (!login) {
            Player player = cache.getPlugin().getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                load(true);
                // Just getting them from the cache when they are online, skip all the other shit and just return this.
                // For performance :)
                if (payload != null) {
                    if (!cache.isCached(uuid)) {
                        cache.cache(payload);
                    }
                    return Optional.of(payload);
                }
            }
        }

        NetworkProfile networkProfile = cache.getNetworkService().get(uuid).orElse(null);
        cache.getErrorService().debug("Caching Payload [network-node] " + uuid.toString() + " (login: " + login + ")");
        if (networkProfile != null) {
            if (networkProfile.isOnlineOtherServer()) {
                PayloadServer server = cache.getServerService().get(networkProfile.getLastSeenServer()).orElse(null);
                if (server != null && server.isOnline()) {
                    // Handshake
                    cache.getErrorService().debug("Handshaking " + uuid.toString() + " from server " + server.getName());
                    HandshakeHandler<ProfileHandshake> handshake = cache.getHandshakeService().publish(new ProfileHandshake(cache.getInjector(), cache, uuid, server.getName()));
                    Optional<ProfileHandshake> o = handshake.waitForReply(cache.getSettings().getHandshakeTimeoutSeconds());
                    if (o.isPresent()) {
                        timeoutAttempts = 0;
                        cache.getErrorService().debug("Handshake complete for " + uuid.toString() + ", loading from DB");
                        load(false);
                    } else {
                        // Timed out
                        timeoutAttempts++;
                        if (timeoutAttempts >= cache.getSettings().getHandshakeTimeOutAttemptsAllowJoin()) {
                            timeoutAttempts = 0;
                            load(false);
                            // They timed out past the max threshold specified, allow them to join / load from database
                        } else {
                            denyJoin = true;
                            joinDenyReason = ChatColor.RED + "Timed out while loading your profile.  Please try again.";
                            cache.getErrorService().debug("Handshake timed out for " + uuid.toString());
                        }
                    }
                } else {
                    // Target server isn't online, or there is no recent server
                    cache.getErrorService().debug("Target server '" + (server != null ? server.getName() : "n/a") + "' not online for handshake for " + uuid.toString(), ", loading from database");
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

        if (payload != null) {
            if (login || cache.getSettings().isAlwaysCacheOnLoadNetworkNode()) {
                cache.cache(payload);
            }
            if (username != null && !payload.getUsername().equalsIgnoreCase(username)) {
                cache.getErrorService().debug("Updated username: " + payload.getUsername() + " to " + username);
                payload.setUsername(username);
                if (!cache.save(payload)) {
                    cache.getErrorService().capture("Error saving Payload during caching after username update: " + payload.getUsername());
                }
            }
            if (login) {
                timeoutAttempts = 0;
                if (networkProfile == null) {
                    networkProfile = cache.getNetworkService().create(payload);
                }

                networkProfile.markLoaded(true);
                NetworkProfile finalNetworkProfile = networkProfile;
                cache.runAsync(() -> cache.getNetworkService().save(finalNetworkProfile));
            }
        }
        return Optional.ofNullable(payload);
    }

    public void initializeOnJoin(Player player) {
        this.player = player;
        if (payload == null) {
            payload = cache.getFromCache(player).orElse(null);
        }
        if (payload != null) {
            cache.getErrorService().debug("called initializeOnJoin() in controller for " + player.getName());
            payload.initializePlayer(player);
        } else {
            cache.getErrorService().debug("failed to call initializeOnJoin() for " + player.getName() + " (payload is null in controller)");
        }
    }

}
