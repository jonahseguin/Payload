/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;
import com.jonahseguin.payload.database.PayloadDatabase;

public class CmdDatabaseList implements PayloadCommand {

    private final PayloadAPI api;

    @Inject
    public CmdDatabaseList(PayloadAPI api) {
        this.api = api;
    }

    @Override
    public void execute(CmdArgs args) {
        args.msg("&7***** &6Payload Databases &7*****");
        for (PayloadDatabase database : api.getDatabases().values()) {
            args.msg("&7" + database.getName() + " - " + (database.getState().isDatabaseConnected() ? "&aConnected" : "&cDisconnected"));
        }
    }

    @Override
    public String name() {
        return "databases";
    }

    @Override
    public String[] aliases() {
        return new String[]{"dbs", "dblist", "databaselist"};
    }

    @Override
    public String desc() {
        return "View registered databases";
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
