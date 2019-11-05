/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.listener;

import com.jonahseguin.lang.LangDefinitions;
import com.jonahseguin.lang.LangModule;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

public class LockListener implements Listener, LangModule {

    private final PayloadPlugin payloadPlugin;

    public LockListener(PayloadPlugin payloadPlugin) {
        this.payloadPlugin = payloadPlugin;
        payloadPlugin.getLang().register(this);
    }

    @Override
    public void define(LangDefinitions l) {
        l.define("kick", "&cThe server is currently locked for maintenance.  Please try again soon.  If the server just started up, wait a few seconds for startup to complete and try again.");
        l.define("bypassed", "&c[Payload] Locked for maintenance, but you bypassed this lock because you have admin privileges.");
    }

    @Override
    public String langModule() {
        return "lock";
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLoginPayloadLocked(PlayerLoginEvent event) {
        if (payloadPlugin.isLocked()) {
            Player player = event.getPlayer();
            if (!PayloadPermission.ADMIN.has(player)) {
                event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, payloadPlugin.getLang().module(this).format("kick"));
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
                player.sendMessage(payloadPlugin.getLang().module(this).format("bypassed"));
            }
        }
    }

}
