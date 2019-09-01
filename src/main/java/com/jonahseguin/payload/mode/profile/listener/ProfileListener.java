package com.jonahseguin.payload.mode.profile.listener;

import com.jonahseguin.payload.PayloadAPI;
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
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, c.getLangController().get(PLang.KICK_MESSAGE_LOCKED));
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
        // TODO: Save, remove their controller, clear their data
        PayloadAPI.get().getCaches().values().forEach(c -> {
            if (c instanceof ProfileCache) {
                ProfileCache cache = (ProfileCache) c;
                cache.removeData(player.getUniqueId());
                cache.removeController(player.getUniqueId());
            }
        });
    }

}
