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
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.server.PayloadServer;

public class CmdServers implements PayloadCommand {

    private final PayloadAPI api;

    @Inject
    public CmdServers(PayloadAPI api) {
        this.api = api;
    }

    @Override
    public void execute(CmdArgs args) {
        String dbName = args.joinArgs();
        DatabaseService database = api.getDatabase(dbName);
        if (database == null) {
            args.msg("4");
            args.msg("&cA database with the name '{0}' does not exist.  Type /payload databases for a list of databases.", dbName);
            return;
        }

        args.msg("&7***** &6Payload Servers in Database: {0} &7*****", database.getName());
        for (PayloadServer server : database.getServerService().getServers()) {
            args.msg("&7- &e{0} &7- {1} &7- Last pinged &6{2}", server.getName(), (server.isOnline() ? "&aOnline" : "&cOffline"), convertPing(server.getLastPing()));
        }
    }

    private String convertPing(long lastPing) {
        long diff = System.currentTimeMillis() - lastPing;
        int seconds = (int) (diff / 1000);
        if (seconds > 600) {
            return "more than 10 minutes ago";
        }
        return seconds + " seconds ago";
    }

    @Override
    public String name() {
        return "servers";
    }

    @Override
    public String[] aliases() {
        return new String[]{"serves", "serverlist", "listservers", "dbservers"};
    }

    @Override
    public String desc() {
        return "View servers registered in a database";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.ADMIN;
    }

    @Override
    public String usage() {
        return "<database name>";
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
