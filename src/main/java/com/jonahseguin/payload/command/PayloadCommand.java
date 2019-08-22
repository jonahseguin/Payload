package com.jonahseguin.payload.command;

import com.jonahseguin.payload.base.PayloadPermission;

public interface PayloadCommand {

    void execute(CmdArgs args);

    String name();

    String[] aliases();

    String desc();

    PayloadPermission permission();

    String usage();

    boolean playerOnly();

}
