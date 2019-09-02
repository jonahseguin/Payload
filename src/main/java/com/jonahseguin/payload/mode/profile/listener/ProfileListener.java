package com.jonahseguin.payload.mode.profile.listener;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.lang.PLang;
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

import java.util.UUID;

public class ProfileListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onCachingStart(AsyncPlayerPreLoginEvent event) {
        final String username = event.getName();
        final UUID uniqueId = event.getUniqueId();
        final String ip = event.getAddress().getHostAddress();

        for (PayloadCache c : PayloadAPI.get().getCaches().values()) {
            if (c instanceof ProfileCache) {
                if (c.getState().isLocked()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, c.getLangController().get(PLang.KICK_MESSAGE_LOCKED, c.getName()));
                    return; // Stop here
                }
            }
        }

        PayloadAPI.get().getCaches().values().forEach(c -> {
            if (c instanceof ProfileCache) {
                ProfileCache cache = (ProfileCache) c;
                ProfileData data = cache.createData(username, uniqueId, ip);
                PayloadProfileController controller = cache.controller(data);
                controller.cache();

                if (controller.isDenyJoin()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, controller.getJoinDenyReason());
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCachingInit(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        PayloadAPI.get().getCaches().values().forEach(c -> {
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
        PayloadAPI.get().getCaches().values().forEach(c -> {
            if (c instanceof ProfileCache) {
                ProfileCache cache = (ProfileCache) c;

                cache.removeData(player.getUniqueId());
                cache.removeController(player.getUniqueId());

                if (cache.getMode().equals(PayloadMode.STANDALONE)) {
                    // save on quit in standalone mode
                    cache.getPool().submit(() -> {
                        if (!cache.save(player)) {
                            cache.getErrorHandler().debug(cache, "Player could not be saved on quit (not cached): " + player.getName());
                        }
                    });
                } else if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
                    // In network node mode, join is handled before quit when switching servers
                    // so we don't want to save on quit
                    // but we do want to remove their locally cached profile because the data will be outdated
                    // and we want to prevent accidental data rollbacks
                    cache.getLocalLayer().remove(player.getUniqueId());
                }
            }
        });
    }

}
