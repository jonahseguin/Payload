package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdCacheLayer implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        String cacheName = args.joinArgs();
        PayloadCache cache = PayloadAPI.get().getCache(cacheName);
        if (cache == null) {
            args.msg("4");
            args.msg("&cA cache with the name '{0}' does not exist.  Type /payload caches for a list of caches.", cacheName);
            return;
        }

        args.msg("&7***** &6Payload Cache: {0} &7*****", cacheName);
        args.msg("&7{0} objects currently cached", cache.cachedObjectCount() + "");
        args.msg("&c{0} objects failed to cache", cache.getFailureManager().getFailures().size() + "");
        args.msg("&7Current State: {0}", cache.getState().isLocked() ? "&cLocked" : "&aUnlocked");
        if (cache.getPayloadDatabase() != null) {
            if (cache.requireMongoDb()) {
                args.msg("&7MongoDB Status: {0} &7(Init: {1}&7)", (cache.getPayloadDatabase().getState().isMongoConnected() ? "&aConnected" : "&cDisconnected"), (cache.getPayloadDatabase().getState().isMongoInitConnect() ? "&aYes" : "&cNo"));
            }
            if (cache.requireRedis()) {
                args.msg("&7Redis Status: {0} &7(Init: {1}&7)", (cache.getPayloadDatabase().getState().isRedisConnected() ? "&aConnected" : "&cDisconnected"), (cache.getPayloadDatabase().getState().isRedisInitConnect() ? "&aYes" : "&cNo"));
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
        return 2;
    }
}
