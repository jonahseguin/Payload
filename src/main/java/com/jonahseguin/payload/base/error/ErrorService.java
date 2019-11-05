package com.jonahseguin.payload.base.error;

import com.jonahseguin.lang.LangModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ErrorService {

    void capture(@Nonnull Throwable throwable);

    void capture(@Nonnull Throwable throwable, @Nonnull String msg);

    void capture(@Nonnull String msg);

    void capture(@Nonnull String module, @Nonnull String key, @Nullable Object... args);

    void capture(@Nonnull Throwable throwable, @Nonnull String module, @Nonnull String key, @Nullable Object... args);

    void capture(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args);

    void capture(@Nonnull Throwable throwable, @Nonnull LangModule module, @Nonnull String key, @Nullable Object... args);

    void debug(@Nonnull String msg);

    void debug(@Nonnull String module, @Nonnull String key, @Nullable Object... args);

    void debug(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args);

}
