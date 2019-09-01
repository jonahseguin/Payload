package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdCacheList implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        args.msg("&7***** &6Payload Caches &7*****");
        for (PayloadCache cache : PayloadAPI.get().getCaches().values()) {
            args.msg("&7" + cache.getName() + " - " + cache.getMode().toString().toLowerCase() + " - " + cache.cachedObjectCount() + " objects");
        }
    }

    @Override
    public String name() {
        return "caches";
    }

    @Override
    public String[] aliases() {
        return new String[]{"cs", "cachelist"};
    }

    @Override
    public String desc() {
        return "View active caches";
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
}
