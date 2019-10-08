/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CmdProfile implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        String cacheName = args.joinArgs(0, args.length() - 1);
        PayloadCache cache = PayloadAPI.get().getCache(cacheName);
        if (cache == null) {
            args.msg("&cA cache with the name '{0}' does not exist.  Type /payload caches for a list of caches.", cacheName);
            return;
        }
        if (cache instanceof ProfileCache) {
            ProfileCache pc = (ProfileCache) cache;

            String playerName = args.arg(args.length() - 1);
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                playerName = player.getName();
            }

            PayloadProfile profile = pc.getProfileByName(playerName);
            if (profile != null) {
                args.msg("&7***** &6Payload Profile: {0} &7*****", playerName);
                args.msg("&7UUID: &6{0}", profile.getUniqueId().toString());
                args.msg("&7Online: {0}", (profile.isOnline() ? "&aYes" : "&cNo"));
                args.msg("&7Last Seen On: {0}", (profile.getLastSeenServer() != null ? "&6" + profile.getLastSeenServer() : "&cN/A"));
                args.msg("&7Last Seen At: {0}", (profile.isOnline() ? "&aNow" : "&6" + formatDateTime(profile.getLastSeenTimestamp())));
                args.msg("&7Last Saved: {0}", (profile.getLastSaveTimestamp() > 0 ? "&6" + formatDateTime(profile.getLastSeenTimestamp()) : "&cNever"));
                args.msg("&7Last Save Status: {0}", (profile.isSaveFailed() ? "&cFailed" : "&aSuccessful"));
                args.msg("&7Loading Source: &6{0}", profile.getLoadingSource());
                args.msg("&7Login IP: {0}", (profile.getLoginIp() != null ? "&6" + profile.getLoginIp() : "&cN/A"));
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
