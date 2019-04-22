package com.jonahseguin.payload.base.exception.runtime;

import com.jonahseguin.payload.base.PayloadCache;

public class PayloadDatabaseException extends PayloadRuntimeException {

    public PayloadDatabaseException(PayloadCache cache) {
        super(cache);
    }

    public PayloadDatabaseException(String message, PayloadCache cache) {
        super(message, cache);
    }

    public PayloadDatabaseException(String message, Throwable cause, PayloadCache cache) {
        super(message, cause, cache);
    }

    public PayloadDatabaseException(Throwable cause, PayloadCache cache) {
        super(cause, cache);
    }

    public PayloadDatabaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, PayloadCache cache) {
        super(message, cause, enableSuppression, writableStackTrace, cache);
    }
}
