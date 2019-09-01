package com.jonahseguin.payload.command.commands;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.command.CmdArgs;
import com.jonahseguin.payload.command.PayloadCommand;

public class CmdCacheLayers implements PayloadCommand {

    @Override
    public void execute(CmdArgs args) {
        String cacheName = args.joinArgs();
        PayloadCache cache = PayloadAPI.get().getCache(cacheName);
        if (cache == null) {
            args.msg("&cA cache with the name '{0}' does not exist.  Type /payload caches for a list of caches.", cacheName);
            return;
        }

        args.msg("&7***** &6Payload Cache Layers: {0} &7*****", cacheName);
        for (Object o : cache.getLayerController().getLayers()) {
            if (o instanceof PayloadLayer) {
                PayloadLayer layer = (PayloadLayer) o;
                args.msg("&7- " + layer.layerName());
            }
        }
    }

    @Override
    public String name() {
        return "layers";
    }

    @Override
    public String[] aliases() {
        return new String[]{"cl", "cachelayers"};
    }

    @Override
    public String desc() {
        return "View layers for a cache";
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
}
