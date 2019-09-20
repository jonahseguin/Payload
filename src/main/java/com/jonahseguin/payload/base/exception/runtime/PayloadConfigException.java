package com.jonahseguin.payload.base.exception.runtime;

import com.jonahseguin.payload.base.PayloadCache;

public class PayloadConfigException extends PayloadRuntimeException {

    public PayloadConfigException(PayloadCache cache) {
        super(cache);
    }

    public PayloadConfigException(String message) {
        super(message);
    }

    public PayloadConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public PayloadConfigException(String message, PayloadCache cache) {
        super(message, cache);
    }

    public PayloadConfigException(String message, Throwable cause, PayloadCache cache) {
        super(message, cause, cache);
    }

    public PayloadConfigException(Throwable cause, PayloadCache cache) {
        super(cause, cache);
    }

    public PayloadConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, PayloadCache cache) {
        super(message, cause, enableSuppression, writableStackTrace, cache);
    }
}
