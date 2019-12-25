/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.error;

import com.jonahseguin.payload.base.Cache;

import javax.annotation.Nonnull;

public class CacheErrorService implements ErrorService {

    protected final Cache cache;

    public CacheErrorService(Cache cache) {
        this.cache = cache;
    }

    public boolean isDebug() {
        return cache.isDebug() || cache.getApi().getPlugin().isDebug();
    }

    public String getMsg(String msg) {
        return "[" + cache.getName() + "] " + msg;
    }

    @Override
    public void capture(@Nonnull Throwable throwable) {
        cache.getPlugin().getLogger().severe(getMsg(throwable.getMessage()));
        if (isDebug()) {
            throwable.printStackTrace();
        }
    }


    @Override
    public void capture(@Nonnull Throwable throwable, @Nonnull String msg) {
        cache.getPlugin().getLogger().severe(msg + " - " + getMsg(throwable.getMessage()));
        if (isDebug()) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void capture(@Nonnull String msg) {
        cache.getPlugin().getLogger().severe(getMsg(msg));
    }


    @Override
    public void debug(@Nonnull String msg) {
        cache.getPlugin().getLogger().info("[Debug]" + getMsg(msg));
    }
}
