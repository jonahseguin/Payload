/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

import com.google.inject.Inject;
import com.jonahseguin.lang.LangDefinitions;
import com.jonahseguin.lang.LangModule;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.error.ErrorService;
import com.jonahseguin.payload.base.lang.LangService;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DatabaseErrorService implements ErrorService, LangModule {

    protected final String name;
    protected final LangService lang;
    protected final Plugin plugin;
    protected final PayloadPlugin payloadPlugin;
    
    @Inject
    public DatabaseErrorService(@Database String name, LangService lang, Plugin plugin, PayloadPlugin payloadPlugin) {
        this.name = name;
        this.lang = lang;
        this.plugin = plugin;
        this.payloadPlugin = payloadPlugin;
    }

    @Override
    public void define(LangDefinitions l) {
        l.define("error-generic", "&7[Payload][&cError&7][{0}] {1}");
        l.define("error-specific", "&7[Payload][&cError&7][{0}] {1} - {2}");
        l.define("debug", "&7[Payload][&eDebug&7][{0}] {1}");
    }

    @Override
    public String langModule() {
        return "database";
    }

    @Override
    public String capture(@Nonnull Throwable throwable) {
        String s = lang.module(this).format("error-generic", name, throwable.getMessage());
        plugin.getLogger().severe(s);
        if (payloadPlugin.isDebug()) {
            throwable.printStackTrace();
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull Throwable throwable, @Nonnull String msg) {
        String s = lang.module(this).format("error-specific", name, throwable.getMessage(), msg);
        plugin.getLogger().severe(s);
        if (payloadPlugin.isDebug()) {
            throwable.printStackTrace();
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull String msg) {
        String s = lang.module(this).format("error-generic", name, msg);
        plugin.getLogger().severe(s);
        if (payloadPlugin.isDebug()) {
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull String module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        plugin.getLogger().severe(s);
        if (payloadPlugin.isDebug()) {
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull Throwable throwable, @Nonnull String module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        plugin.getLogger().severe(s);
        if (payloadPlugin.isDebug()) {
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
            throwable.printStackTrace();
        }
        return s;
    }

    @Override
    public String capture(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        plugin.getLogger().severe(s);
        if (payloadPlugin.isDebug()) {
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull Throwable throwable, @Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        plugin.getLogger().severe(s);
        if (payloadPlugin.isDebug()) {
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
            throwable.printStackTrace();
        }
        return s;
    }

    @Override
    public String debug(@Nonnull String msg) {
        String s = lang.module(this).format("debug", name, msg);
        plugin.getLogger().info(s);
        if (payloadPlugin.isDebug()) {
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String debug(@Nonnull String module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        plugin.getLogger().info(s);
        if (payloadPlugin.isDebug()) {
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String debug(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        plugin.getLogger().info(s);
        if (payloadPlugin.isDebug()) {
            payloadPlugin.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }
}
