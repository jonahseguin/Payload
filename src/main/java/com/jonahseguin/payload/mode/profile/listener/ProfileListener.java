package com.jonahseguin.payload.mode.profile.listener;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileController;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

public class ProfileListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onCachingStart(AsyncPlayerPreLoginEvent event) {
        final String username = event.getName();
        final UUID uniqueId = event.getUniqueId();
        final String ip = event.getAddress().getHostAddress();

        List<PayloadCache> sortedCaches = PayloadAPI.get().getSortedCachesByDepends();

        for (PayloadCache c : sortedCaches) {
            if (c instanceof ProfileCache) {
                if (c.getState().isLocked()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, c.getLangController().get(PLang.KICK_MESSAGE_LOCKED, c.getName()));
                    c.getErrorHandler().debug(c, "Denied join (locked) for " + username);
                    return; // Stop here
                }
            }
        }

        sortedCaches.forEach(c -> {
            if (c instanceof ProfileCache) {
                ProfileCache cache = (ProfileCache) c;
                c.getErrorHandler().debug(c, "Starting caching " + username);
                ProfileData data = cache.createData(username, uniqueId, ip);
                PayloadProfileController controller = cache.controller(data);
                controller.cache();
                c.getErrorHandler().debug(c, "Cached " + username);

                if (controller.isDenyJoin()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, controller.getJoinDenyReason());
                    c.getErrorHandler().debug(c, "Denied join for " + username);
                    // We need to reset their controller
                    cache.removeController(uniqueId);
                    cache.removeData(uniqueId);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCachingInit(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        PayloadAPI.get().getSortedCachesByDepends().forEach(c -> {
            if (c instanceof ProfileCache) {
                ProfileCache cache = (ProfileCache) c;
                PayloadProfileController controller = cache.getController(player.getUniqueId());
                if (controller != null) {
                    controller.initializeOnJoin(player);
                }
                if (cache.getFailureManager().hasFailure(player.getUniqueId())) {
                    cache.getFailureManager().getFailedPayload(player.getUniqueId()).setPlayer(player);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PayloadAPI.get().getSortedCachesByDepends().forEach(c -> {
            if (c instanceof ProfileCache) {
                ProfileCache cache = (ProfileCache) c;

                if (cache.getMode().equals(PayloadMode.STANDALONE)) {
                    // save on quit in standalone mode
                    cache.getPool().submit(() -> {
                        cache.getErrorHandler().debug(cache, "Saving player " + player.getName() + " on quit");
                        if (!cache.save(player)) {
                            cache.getErrorHandler().debug(cache, "Player could not be saved on quit (not cached): " + player.getName());
                        }
                    });
                } else if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
                    PayloadProfile profile = cache.getLocalProfile(player);
                    if (profile != null) {
                        if (!profile.isSwitchingServers()) {
                            // Not switching servers (no incoming handshake) -- we can assume they are actually
                            // Logging out, and not switching servers
                            profile.setOnline(false);
                            cache.save(player); // Save
                            cache.getErrorHandler().debug(cache, "Saving player " + player.getName() + " on logout (not switching servers)");
                            // It's safe to save here because they aren't switching servers, but they are logging out entirely
                        } else {
                            cache.getErrorHandler().debug(cache, "Not saving player " + player.getName() + " on quit (is switching servers)");
                        }
                    } else {
                        // This shouldn't happen
                        cache.getErrorHandler().debug(cache, "Profile null during logout for Payload '" + player.getName() + "': could not set online=false");
                    }

                    // In network node mode, join is handled before quit when switching servers
                    // so we don't want to save on quit
                    // but we do want to remove their locally cached profile because the data will be outdated
                    // and we want to prevent accidental data rollbacks
                    cache.getLocalLayer().remove(player.getUniqueId());

                }

                // Remove their data after they leave so that its reset for their next login
                cache.removeData(player.getUniqueId());
                cache.removeController(player.getUniqueId());
            }
        });
    }

}
