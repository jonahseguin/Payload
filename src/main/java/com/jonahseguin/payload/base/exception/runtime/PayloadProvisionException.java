package com.jonahseguin.payload.base.exception.runtime;

import com.jonahseguin.payload.base.PayloadCache;

public class PayloadProvisionException extends PayloadRuntimeException {

    public PayloadProvisionException(String message) {
        super(message);
    }

    public PayloadProvisionException(PayloadCache cache) {
        super(cache);
    }

    public PayloadProvisionException(Throwable cause) {
        super(cause);
    }

    public PayloadProvisionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PayloadProvisionException(String message, PayloadCache cache) {
        super(message, cache);
    }

    public PayloadProvisionException(String message, Throwable cause, PayloadCache cache) {
        super(message, cause, cache);
    }

    public PayloadProvisionException(Throwable cause, PayloadCache cache) {
        super(cause, cache);
    }

    public PayloadProvisionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, PayloadCache cache) {
        super(message, cause, enableSuppression, writableStackTrace, cache);
    }
}
