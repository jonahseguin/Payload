package com.jonahseguin.payload.database;

import com.google.inject.Inject;
import com.jonahseguin.lang.LangModule;
import com.jonahseguin.payload.annotation.Database;
import com.jonahseguin.payload.base.error.ErrorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DatabaseErrorService implements ErrorService {

    private final String name;

    @Inject
    public DatabaseErrorService(@Database String name) {
        this.name = name;
    }

    @Override
    public void capture(@Nonnull Throwable throwable) {

    }

    @Override
    public void capture(@Nonnull Throwable throwable, @Nonnull String msg) {

    }

    @Override
    public void capture(@Nonnull String msg) {

    }

    @Override
    public void capture(@Nonnull String module, @Nonnull String key, @Nullable Object... args) {

    }

    @Override
    public void capture(@Nonnull Throwable throwable, @Nonnull String module, @Nonnull String key, @Nullable Object... args) {

    }

    @Override
    public void capture(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {

    }

    @Override
    public void capture(@Nonnull Throwable throwable, @Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {

    }

    @Override
    public void debug(@Nonnull String msg) {

    }

    @Override
    public void debug(@Nonnull String module, @Nonnull String key, @Nullable Object... args) {

    }

    @Override
    public void debug(@Nonnull LangModule module, @Nonnull String key, @Nullable Object... args) {

    }
}
