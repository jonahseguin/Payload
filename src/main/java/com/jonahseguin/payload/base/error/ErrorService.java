/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.error;

import com.jonahseguin.lang.LangModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ErrorService {

    String capture(@Nonnull Throwable throwable);

    String capture(@Nonnull Throwable throwable, @Nonnull String msg);

    String capture(@Nonnull String msg);

    String capture(@Nonnull String module, @Nonnull String key, @Nullable Object... args);

    String capture(@Nonnull Throwable throwable, @Nonnull String module, @Nonnull String key, @Nullable Object... args);

    String capture(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args);

    String capture(@Nonnull Throwable throwable, @Nonnull LangModule module, @Nonnull String key, @Nullable Object... args);

    String debug(@Nonnull String msg);

    String debug(@Nonnull String module, @Nonnull String key, @Nullable Object... args);

    String debug(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args);

}
