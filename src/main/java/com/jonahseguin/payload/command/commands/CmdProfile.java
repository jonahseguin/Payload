/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;
import com.jonahseguin.payload.mode.profile.NetworkProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class CmdProfile implements PayloadCommand {

    private final PayloadAPI api;

    @Inject
    public CmdProfile(PayloadAPI api) {
        this.api = api;
    }

    @Override
    public void execute(CmdArgs args) {
        String cacheName = args.joinArgs(0, args.length() - 1);
        Cache cache = api.getCache(cacheName);
        if (cache == null) {
            args.msg("&cA cache with the name '{0}' does not exist.  Type /payload caches for a list of caches.", cacheName);
            return;
        }
        if (cache instanceof PayloadProfileCache) {
            PayloadProfileCache pc = (PayloadProfileCache) cache;

            String playerName = args.arg(args.length() - 1);
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                playerName = player.getName();
            }

            Optional<PayloadProfile> o = pc.get(playerName);
            if (o.isPresent()) {
                PayloadProfile profile = o.get();
                args.msg("&7***** &6Payload Profile: {0} &7*****", playerName);
                args.msg("&7UUID: &6{0}", profile.getUniqueId().toString());
                args.msg("&7Player Online (this server): {0}", (profile.isOnline() ? "&aYes" : "&cNo"));
                args.msg("&7Last Save Status: {0}", (profile.isSaveFailed() ? "&cFailed" : "&aSuccessful"));
                args.msg("&7Loading Source: &6{0}", profile.getLoadingSource());
                args.msg("&7Login IP: {0}", (profile.getLoginIp() != null ? "&6" + profile.getLoginIp() : "&cN/A"));
                Optional<NetworkProfile> onp = pc.getNetworked(profile);
                if (onp.isPresent()) {
                    NetworkProfile np = onp.get();
                    args.msg("&eNetwork Properties:");
                    args.msg("&7Online: &6{0}", (np.isOnline() ? "&aYes" : "&cNo"));
                    args.msg("&7Loaded: &6{0}", (np.isLoaded() ? "&aYes" : "&cNo"));
                    args.msg("&7Last Seen On: &6{0}", np.getLastSeenServer() != null ? np.getLastSeenServer() : "&cN/A");
                    args.msg("&7Last Seen At: &6{0}", np.getLastSeen() != null ? np.getLastSeen().toString() : "&cN/A");
                    args.msg("&7Last Saved: &6{0}", np.getLastSaved() != null ? np.getLastSaved().toString() : "&cN/A");
                    args.msg("&7Last Cached: &6{0}", np.getLastCached() != null ? np.getLastCached().toString() : "&cN/A");
                }
            } else {
                args.msg("&cPayload: A profile with username '{0}' does not exist in cache '{1}'.", playerName);
            }
        } else {
            args.msg("&cPayload: The '{0}' cache is not a profile cache.", cache.getName());
        }

    }

    public String formatDateTime(long time) {
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd @ hh:mm:ss a");
        return format.format(date);
    }

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public String[] aliases() {
        return new String[]{"p", "lookup", "pro"};
    }

    @Override
    public String desc() {
        return "View profiles for a player";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.ADMIN;
    }

    @Override
    public String usage() {
        return "<cache name> <player name>";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public int minArgs() {
        return 2;
    }
}
