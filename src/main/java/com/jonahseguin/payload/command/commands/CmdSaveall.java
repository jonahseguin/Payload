package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdSaveall implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        args.msg("&7Payload: &6Saving all payloads in all active caches...");
        PayloadPlugin.get().getServer().getScheduler().runTaskAsynchronously(PayloadPlugin.get(), () -> {
            PayloadAPI.get().getCaches().values().forEach(cache -> {
                int failures = cache.saveAll();
                args.msg("&7Save complete for cache '&6{0}&7' with {1} &7failures", cache.getName(), (failures > 0 ? "&c" + failures : "&a" + failures));
            });
            args.msg("&7Payload: &6Save-all complete.");
        });
    }

    @Override
    public String name() {
        return "saveall";
    }

    @Override
    public String[] aliases() {
        return new String[]{"forcesave", "savea", "save-all"};
    }

    @Override
    public String desc() {
        return "Save all payloads in all active caches";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.ADMIN;
    }

    @Override
    public String usage() {
        return "";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public int minArgs() {
        return 0;
    }
}
