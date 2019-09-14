package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;

public class CmdSetID implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        String name = args.arg(0);
        if (StringUtils.isAlphanumeric(name)) {
            PayloadPlugin.get().getLocal().savePayloadID(name);
            for (PayloadCache cache : PayloadAPI.get().getCaches().values()) {
                cache.updatePayloadID();
            }
            args.msg("&7[Payload] &aPayload-ID set to: " + PayloadAPI.get().getPayloadID());
        } else {
            args.msg(ChatColor.RED + "Server name must be alphanumeric");
        }
    }

    @Override
    public String name() {
        return "setid";
    }

    @Override
    public String[] aliases() {
        return new String[]{"setname", "set"};
    }

    @Override
    public String desc() {
        return "Set the server name/Payload-ID for this server";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.ADMIN;
    }

    @Override
    public String usage() {
        return "<name>";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public int minArgs() {
        return 1;
    }

}
