/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.listener;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class LockListener implements Listener {

    private final PayloadPlugin payloadPlugin;

    public LockListener(PayloadPlugin payloadPlugin) {
        this.payloadPlugin = payloadPlugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginPayloadLocked(PlayerLoginEvent event) {
        if (payloadPlugin.isLocked()) {
            Player player = event.getPlayer();
            if (!PayloadPermission.ADMIN.has(player)) {
                event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, payloadPlugin.getLangController().get(PLang.KICK_MESSAGE_LOCKED));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinPayloadLocked(PlayerJoinEvent event) {
        if (payloadPlugin.isLocked()) {
            Player player = event.getPlayer();
            if (PayloadPermission.ADMIN.has(player)) {
                player.sendMessage(" ");
                player.sendMessage(" ");
                player.sendMessage(payloadPlugin.getLangController().get(PLang.KICK_MESSAGE_ADMIN_LOCKED));
            }
        }
    }

}
