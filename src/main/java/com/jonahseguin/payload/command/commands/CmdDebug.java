/*
 * Copyright (c) 2020 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdDebug implements PayloadCommand {

    private final PayloadPlugin payloadPlugin;
    private final PayloadAPI api;

    @Inject
    public CmdDebug(PayloadPlugin payloadPlugin, PayloadAPI api) {
        this.payloadPlugin = payloadPlugin;
        this.api = api;
    }

    @Override
    public void execute(CmdArgs args) {
        if (args.length() == 0) {
            args.msg("&7Payload: Debug is {0} &7(global debug can only be toggled from the config)", (payloadPlugin.isDebug() ? "&aon" : "&coff"));
            args.msg("&7Use /payload debug <cache> to toggle debug for a specific cache.");
        } else {
            String name = args.arg(0);
            Cache cache = api.getCache(name);
            cache.setDebug(!cache.isDebug());
            args.msg("&7Payload: Debug is now {0} &7for the cache: '&6" + cache.getName() + "&7'", (cache.isDebug() ? "&aon" : "&coff"));
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
        return "Toggle debug status for a cache";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.DEBUG;
    }

    @Override
    public String usage() {
        return "<cache>";
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
