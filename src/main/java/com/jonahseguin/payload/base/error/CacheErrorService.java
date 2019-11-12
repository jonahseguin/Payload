/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.error;

import com.google.inject.Inject;
import com.jonahseguin.lang.LangDefinitions;
import com.jonahseguin.lang.LangModule;
import com.jonahseguin.payload.base.PayloadCacheService;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.LangService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CacheErrorService implements ErrorService, LangModule {

    protected final PayloadCacheService cache;
    protected final LangService lang;

    @Inject
    public CacheErrorService(PayloadCacheService cache, LangService lang) {
        this.cache = cache;
        this.lang = lang;
        cache.getLang().register(this);
    }

    @Override
    public void define(LangDefinitions l) {
        l.define("error-generic", "&7[Payload][&cError&7][{0}] {1}");
        l.define("error-specific", "&7[Payload][&cError&7][{0}] {1} - {2}");
        l.define("debug", "&7[Payload][&eDebug&7][{0}] {1}");
    }

    @Override
    public String langModule() {
        return "error";
    }

    @Override
    public String capture(@Nonnull Throwable throwable) {
        String s = lang.module(this).format("error-generic", cache.getName(), throwable.getMessage());
        cache.getPlugin().getLogger().severe(s);
        if (cache.isDebug()) {
            throwable.printStackTrace();
            cache.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull Throwable throwable, @Nonnull String msg) {
        String s = lang.module(this).format("error-specific", cache.getName(), throwable.getMessage(), msg);
        cache.getPlugin().getLogger().severe(s);
        if (cache.isDebug()) {
            throwable.printStackTrace();
            cache.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull String msg) {
        String s = lang.module(this).format("error-generic", cache.getName(), msg);
        cache.getPlugin().getLogger().severe(s);
        if (cache.isDebug()) {
            cache.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull String module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        cache.getPlugin().getLogger().severe(s);
        if (cache.isDebug()) {
            cache.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull Throwable throwable, @Nonnull String module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        cache.getPlugin().getLogger().severe(s);
        if (cache.isDebug()) {
            cache.alert(PayloadPermission.DEBUG, s);
            throwable.printStackTrace();
        }
        return s;
    }

    @Override
    public String capture(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        cache.getPlugin().getLogger().severe(s);
        if (cache.isDebug()) {
            cache.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String capture(@Nonnull Throwable throwable, @Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        cache.getPlugin().getLogger().severe(s);
        if (cache.isDebug()) {
            cache.alert(PayloadPermission.DEBUG, s);
            throwable.printStackTrace();
        }
        return s;
    }

    @Override
    public String debug(@Nonnull String msg) {
        String s = lang.module(this).format("debug", cache.getName(), msg);
        cache.getPlugin().getLogger().info(s);
        if (cache.isDebug()) {
            cache.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String debug(@Nonnull String module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        cache.getPlugin().getLogger().info(s);
        if (cache.isDebug()) {
            cache.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }

    @Override
    public String debug(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {
        String s = lang.module(module).format(key, args);
        cache.getPlugin().getLogger().info(s);
        if (cache.isDebug()) {
            cache.alert(PayloadPermission.DEBUG, s);
        }
        return s;
    }
}
