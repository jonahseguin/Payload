/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.error;


import javax.annotation.Nonnull;

public interface ErrorService {

    void capture(@Nonnull Throwable throwable);

    void capture(@Nonnull Throwable throwable, @Nonnull String msg);

    void capture(@Nonnull String msg);

    void debug(@Nonnull String msg);

}
