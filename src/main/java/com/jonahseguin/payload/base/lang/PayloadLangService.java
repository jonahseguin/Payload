/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.lang;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jonahseguin.lang.Lang;
import com.jonahseguin.lang.LangDefinitions;
import com.jonahseguin.lang.LangModule;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Singleton
public class PayloadLangService implements LangService {

    private final Lang lang;

    @Inject
    public PayloadLangService(@Nonnull Plugin plugin) {
        Preconditions.checkNotNull(plugin);
        this.lang = new Lang(plugin);
        lang.load();
        lang.save();
    }

    @Override
    public void register(LangModule module) {
        lang.register(module);
    }

    @Nullable
    @Override
    public String get(@Nonnull String module, @Nonnull String key, @Nullable Object... args) {
        return lang.module(module).format(key, args);
    }

    @Override
    public LangDefinitions module(@Nonnull LangModule module) {
        return lang.module(module);
    }

    @Override
    public LangDefinitions module(@Nonnull String module) {
        return lang.module(module);
    }

    @Override
    public Lang lang() {
        return lang;
    }
}
