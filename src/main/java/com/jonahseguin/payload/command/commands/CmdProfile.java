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

public class CmdProfile implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        String cacheName = args.joinArgs(0, args.length() - 1);
        PayloadCache cache = PayloadAPI.get().getCache(cacheName);
        if (cache == null) {
            args.msg("4");
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
                args.msg("&7Last Seen: {0}", (profile.getLastSeenServer() != null ? "&6" + profile.getLastSeenServer() : "&cN/A"));
                args.msg("&7Login IP: {0}", (profile.getLoginIp() != null ? "&6" + profile.getLoginIp() : "&cN/A"));
            } else {
                args.msg("&cPayload: A profile with username '{0}' does not exist in cache '{1}'.", playerName);
            }
        } else {
            args.msg("&cPayload: The '{0}' cache is not a profile cache.", cache.getName());
        }

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