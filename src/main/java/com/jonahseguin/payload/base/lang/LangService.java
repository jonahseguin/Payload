/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.lang;

import com.jonahseguin.lang.Lang;
import com.jonahseguin.lang.LangDefinitions;
import com.jonahseguin.lang.LangModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface LangService {

    @Nullable
    String get(@Nonnull String module, @Nonnull String key, @Nullable Object... args);

    LangDefinitions module(@Nonnull String module);

    LangDefinitions module(@Nonnull LangModule module);

    Lang lang();

}
