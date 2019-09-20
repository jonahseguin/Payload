package com.jonahseguin.payload.base.error;

import com.jonahseguin.payload.base.PayloadCache;

public interface PayloadErrorHandler {

    void debug(PayloadCache cache, String message);

    void error(PayloadCache cache, String message);

    void exception(PayloadCache cache, Throwable throwable);

    void exception(PayloadCache cache, Throwable throwable, String message);

    boolean isDebug();

}
