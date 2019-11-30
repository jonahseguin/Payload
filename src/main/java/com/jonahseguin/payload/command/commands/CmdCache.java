/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.command.commands;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdCache implements PayloadCommand {

    private final PayloadAPI api;

    @Inject
    public CmdCache(PayloadAPI api) {
        this.api = api;
    }

    @Override
    public void execute(CmdArgs args) {
        String cacheName = args.joinArgs();
        Cache cache = api.getCache(cacheName);
        if (cache == null) {
            args.msg("&cA cache with the name '{0}' does not exist.  Type /payload caches for a list of caches.", cacheName);
            return;
        }

        args.msg("&7***** &6Payload Cache: {0} &7*****", cacheName);
        args.msg("&7{0} objects currently cached", cache.cachedObjectCount() + "");
        args.msg("&7Current State: {0}", cache.isRunning() ? "&aRunning" : "&cNot running");
        if (cache.getDatabase() != null) {
            if (cache.requireMongoDb()) {
                args.msg("&7MongoDB Status: {0} &7(Init: {1}&7)", (cache.getDatabase().getState().isMongoConnected() ? "&aConnected" : "&cDisconnected"), (cache.getDatabase().getState().isMongoInitConnect() ? "&aYes" : "&cNo"));
            }
            if (cache.requireRedis()) {
                args.msg("&7Redis Status: {0} &7(Init: {1}&7)", (cache.getDatabase().getState().isRedisConnected() ? "&aConnected" : "&cDisconnected"), (cache.getDatabase().getState().isRedisInitConnect() ? "&aYes" : "&cNo"));
            }
        }
    }

    @Override
    public String name() {
        return "cache";
    }

    @Override
    public String[] aliases() {
        return new String[]{"c", "cinfo", "cacheinfo"};
    }

    @Override
    public String desc() {
        return "View information on a cache";
    }

    @Override
    public PayloadPermission permission() {
        return PayloadPermission.ADMIN;
    }

    @Override
    public String usage() {
        return "<cache name>";
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
