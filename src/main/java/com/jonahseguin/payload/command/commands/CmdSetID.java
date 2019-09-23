/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadAPI;
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
            PayloadAPI.get().setPayloadID(name);
            args.msg("&7[Payload] &aPayload-ID set to: " + PayloadAPI.get().getPayloadID() + ".  A restart may be required for changes to completely take effect.");
        } else {
            args.msg(ChatColor.RED + "Payload-ID must be alphanumeric");
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
