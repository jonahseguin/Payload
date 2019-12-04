/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.listener;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.mode.profile.*;
import com.jonahseguin.payload.mode.profile.event.PayloadProfileLogoutEvent;
import com.jonahseguin.payload.mode.profile.event.PayloadProfileSwitchServersEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ProfileListener implements Listener {

    private final PayloadAPI api;

    @Inject
    public ProfileListener(PayloadAPI api) {
        this.api = api;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingStart(AsyncPlayerPreLoginEvent event) {
        final String username = event.getName();
        final UUID uniqueId = event.getUniqueId();
        final String ip = event.getAddress().getHostAddress();

        List<Cache> sortedCaches = api.getSortedCachesByDepends();

        sortedCaches.forEach(c -> {
            if (c instanceof ProfileCache) {
                ProfileCache cache = (ProfileCache) c;
                PayloadProfileController controller = cache.controller(uniqueId);
                controller.login(username, ip);
                controller.cache();

                if (controller.isDenyJoin()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, controller.getJoinDenyReason());
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingInit(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        api.getSortedCachesByDepends().forEach(c -> {
            if (c instanceof PayloadProfileCache) {
                PayloadProfileCache cache = (PayloadProfileCache) c;
                PayloadProfileController controller = cache.getController(player.getUniqueId());
                if (controller != null) {
                    cache.getErrorService().debug("Initializing player " + player.getName() + " for cache " + cache.getName());
                    controller.initializeOnJoin(player);
                }
                else {
                    cache.getErrorService().capture("Could not initialize player " + player.getName() + " for cache " + cache.getName() + " (controller is null)");
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProfileQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        api.getSortedCachesByDependsReversed().forEach(c -> {
            if (c instanceof PayloadProfileCache) {
                PayloadProfileCache cache = (PayloadProfileCache) c;
                cache.runAsync(() -> {
                    if (cache.getMode().equals(PayloadMode.STANDALONE)) {
                        // save on quit in standalone mode
                        cache.getPool().submit(() -> {
                            Optional<PayloadProfile> o = cache.getFromCache(player.getUniqueId());
                            if (o.isPresent()) {
                                PayloadProfile profile = o.get();

                                PayloadProfileLogoutEvent payloadEvent = new PayloadProfileLogoutEvent(profile);
                                cache.getPlugin().getServer().getPluginManager().callEvent(payloadEvent);

                                Optional<NetworkProfile> oNP = cache.getNetworked(profile);
                                oNP.ifPresent(networkProfile -> {
                                    networkProfile.setOnline(false);
                                    networkProfile.setLastSeen(new Date());
                                    cache.runAsync(() -> cache.getNetworkService().save(networkProfile));
                                });

                                profile.uninitializePlayer();
                                if (!cache.save(profile)) {
                                    cache.getErrorService().capture("Error saving profile on quit: " + player.getName());
                                }
                                cache.removeController(profile.getUniqueId());
                            }
                        });
                    } else if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
                        Optional<PayloadProfile> o = cache.getFromCache(player.getUniqueId());
                        if (o.isPresent()) {
                            PayloadProfile profile = o.get();
                            profile.uninitializePlayer();
                            Optional<NetworkProfile> onp = cache.getNetworked(profile);
                            if (onp.isPresent()) {
                                NetworkProfile networkProfile = onp.get();
                                if (!profile.hasValidHandshake()) {
                                    PayloadProfileLogoutEvent payloadEvent = new PayloadProfileLogoutEvent(profile);
                                    cache.getPlugin().getServer().getPluginManager().callEvent(payloadEvent);

                                    // Not switching servers (no incoming handshake) -- we can assume they are actually
                                    // Logging out, and not switching servers
                                    networkProfile.setOnline(false);
                                    networkProfile.setLastSeen(new Date());

                                    cache.getNetworkService().save(networkProfile);
                                    cache.save(profile); // Don't publish a sync since we're switching servers
                                    // In network node mode, join is handled before quit when switching servers
                                    // so we don't want to save on quit
                                    // but we do want to remove their locally cached profile because the data will be outdated
                                    // and we want to prevent accidental data rollbacks
                                    cache.getLocalStore().remove(player.getUniqueId());
                                    cache.removeController(player.getUniqueId());
                                    cache.getErrorService().debug("Saving player " + player.getName() + " on logout (not switching servers)");
                                } else {
                                    PayloadProfileSwitchServersEvent payloadEvent = new PayloadProfileSwitchServersEvent(profile);
                                    cache.getPlugin().getServer().getPluginManager().callEvent(payloadEvent);

                                    // Switching servers, don't save their data -- just remove
                                    cache.getLocalStore().remove(player.getUniqueId()); // remove on quit to prevent accidental data rollbacks
                                    cache.getErrorService().debug("Not saving player " + player.getName() + " on quit (is switching servers)");
                                }
                            } else {
                                // no network profile?
                                cache.getErrorService().debug("No network profile during logout for Payload" + player.getName());
                            }
                        } else {
                            // This shouldn't happen
                            cache.getErrorService().debug("Profile null during logout for Payload '" + player.getName() + "': could not set online=false");
                        }
                    }
                });
            }
        });
    }

}
