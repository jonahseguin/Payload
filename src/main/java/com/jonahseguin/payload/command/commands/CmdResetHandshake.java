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

public class CmdResetHandshake implements PayloadCommand {

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

                profile.setSwitchingServers(false);
                profile.setOnline(false);
                profile.setLastSeenServer(null);
                profile.setLoginIp(null);
                args.msg("&7Saving...");
                pc.getPool().submit(() -> {
                    pc.save(profile);
                    args.msg("&7Saved '{0}'.  Their login/handshake data has been reset.", profile.getUsername());
                });
            } else {
                args.msg("&cPayload: A profile with username '{0}' does not exist in cache '{1}'.", playerName);
            }
        } else {
            args.msg("&cPayload: The '{0}' cache is not a profile cache.", cache.getName());
        }
    }

    @Override
    public String name() {
        return "reset";
    }

    @Override
    public String[] aliases() {
        return new String[]{"resethandshake", "rhandshake", "rhand"};
    }

    @Override
    public String desc() {
        return "Reset a player's handshake/login data";
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
