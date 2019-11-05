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

public class CmdDatabase implements PayloadCommand {

    private final PayloadAPI api;

    @Inject
    public CmdDatabase(PayloadAPI api) {
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

        args.msg("&7***** &6Payload Database: {0} &7*****", database.getName());
        args.msg("&7Connected: {0}", (database.getState().isDatabaseConnected() ? "&aYes" : "&cNo"));
        args.msg("&7MongoDB: {0}", (database.getState().isMongoConnected() ? "&aConnected" : "&cDisconnected"));
        args.msg("&7Redis: {0}", (database.getState().isRedisConnected() ? "&aConnected" : "&cDisconnected"));
        args.msg("&7Registered Servers: &6{0}", database.getServerService().getServers().size() + "");
        args.msg("&7Use '/payload servers {0}' to view a list of registered servers in this database for this network", database.getName());
    }

    @Override
    public String name() {
        return "database";
    }

    @Override
    public String[] aliases() {
        return new String[]{"db", "dbinfo", "databaseinfo"};
    }

    @Override
    public String desc() {
        return "View information on a database";
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
