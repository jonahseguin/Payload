/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database.error;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.error.ErrorService;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class DatabaseErrorService implements ErrorService {

    protected final String name;
    protected final Plugin plugin;
    protected final PayloadPlugin payloadPlugin;

    @Inject
    public DatabaseErrorService(@Database String name, Plugin plugin, PayloadPlugin payloadPlugin) {
        this.name = name;
        this.plugin = plugin;
        this.payloadPlugin = payloadPlugin;
    }

    public String getMsg(String msg, boolean debug) {
        return "[Database: " + name + "]" + (debug ? "[Debug]" : "") + " " + msg;
    }

    public boolean isDebug() {
        return payloadPlugin.isDebug();
    }

    @Override
    public void capture(@Nonnull Throwable throwable) {
        plugin.getLogger().severe(getMsg(throwable.getMessage(), false));
        if (isDebug()) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void capture(@Nonnull Throwable throwable, @Nonnull String msg) {
        plugin.getLogger().severe(getMsg(msg + " - " + throwable.getMessage(), false));
        if (payloadPlugin.isDebug()) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void capture(@Nonnull String msg) {
        plugin.getLogger().severe(getMsg(msg, false));
    }

    @Override
    public void debug(@Nonnull String msg) {
        plugin.getLogger().info(getMsg(msg, true));
    }

}
