package com.jonahseguin.payload.command.commands;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdHelp implements PayloadCommand {

    private final PayloadPlugin payloadPlugin;

    @Inject
    public CmdHelp(PayloadPlugin payloadPlugin) {
        this.payloadPlugin = payloadPlugin;
    }

    @Override
    public void execute(CmdArgs args) {
        args.msg("&7***** &6Payload &7*****");
        args.msg("&7Version " + payloadPlugin.getDescription().getVersion() + " by Jonah Seguin");
        for (PayloadCommand cmd : payloadPlugin.getCommandHandler().getCommands().values()) {
            if (cmd.permission().has(args.getSender())) {
                args.msg("&7/payload &e" + cmd.name() + " " + cmd.usage() + "&7 - " + cmd.desc());
            }
        }
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String[] aliases() {
        return new String[]{"h", "commands", "cmds"};
    }

    @Override
    public String desc() {
        return "View available commands";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.NONE;
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
