package com.jonahseguin.payload.command.commands;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdCacheStores implements PayloadCommand {

    private final PayloadAPI api;

    @Inject
    public CmdCacheStores(PayloadAPI api) {
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

        args.msg("&7***** &6Payload Cache Stores: {0} &7*****", cacheName);
        args.msg("&7- " + cache.getLocalStore().layerName());
        args.msg("&7- " + cache.getDatabaseStore().layerName());
    }

    @Override
    public String name() {
        return "stores";
    }

    @Override
    public String[] aliases() {
        return new String[]{"st", "cachestores", "store"};
    }

    @Override
    public String desc() {
        return "View stores for a cache";
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
