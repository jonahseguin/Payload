package com.jonahseguin.payload.base.listener;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.PLang;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class LockListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginPayloadLocked(PlayerLoginEvent event) {
        if (PayloadPlugin.get().isLocked()) {
            Player player = event.getPlayer();
            if (!PayloadPermission.ADMIN.has(player)) {
                event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, PayloadPlugin.get().getGlobalLangController().get(PLang.KICK_MESSAGE_LOCKED));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinPayloadLocked(PlayerJoinEvent event) {
        if (PayloadPlugin.get().isLocked()) {
            Player player = event.getPlayer();
            if (PayloadPermission.ADMIN.has(player)) {
                player.sendMessage(" ");
                player.sendMessage(" ");
                player.sendMessage(PayloadPlugin.get().getGlobalLangController().get(PLang.KICK_MESSAGE_ADMIN_LOCKED));
            }
        }
    }

}
