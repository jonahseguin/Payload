package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdDebug implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        if (args.length() == 0) {
            args.msg("&7Payload: Debug is {0} &7(use &e/payload debug [on/off]&7 to toggle)", (PayloadPlugin.get().isDebug() ? "&aon" : "&coff"));
        } else {
            String toggle = args.arg(0).toLowerCase();
            if (toggle.startsWith("on")) {
                PayloadPlugin.get().setDebug(true);
                args.msg("&7Payload: Debug &aon");
            } else if (toggle.startsWith("off")) {
                PayloadPlugin.get().setDebug(true);
                args.msg("&7Payload: Debug &coff");
            }
        }
    }

    @Override
    public String name() {
        return "debug";
    }

    @Override
    public String[] aliases() {
        return new String[]{"deb", "bug"};
    }

    @Override
    public String desc() {
        return "View debug status and toggle";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.DEBUG;
    }

    @Override
    public String usage() {
        return "[on/off]";
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
