package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdServer implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        args.msg("&7***** &6Payload &7*****");
        args.msg("&7Unique Payload-ID for this server:");
        args.msg("&6{0}", PayloadAPI.get().getPayloadID());
        args.msg("&7To set the server name/Payload-ID for this server, use /payload setid <name>");
    }

    @Override
    public String name() {
        return "server";
    }

    @Override
    public String[] aliases() {
        return new String[]{"serv", "info", "id"};
    }

    @Override
    public String desc() {
        return "View information about this Payload instance";
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
