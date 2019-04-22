package com.jonahseguin.payload.base.exception.runtime;

import com.jonahseguin.payload.base.PayloadCache;

public class PayloadRuntimeException extends RuntimeException {

    public PayloadRuntimeException(PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception");
    }

    public PayloadRuntimeException(String message) {
        super("Payload exception: " + message);
    }

    public PayloadRuntimeException(String message, Throwable cause) {
        super("Payload exception: " + message, cause);
    }

    public PayloadRuntimeException(String message, PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception: " + message);
    }

    public PayloadRuntimeException(String message, Throwable cause, PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception: " + message, cause);
    }

    public PayloadRuntimeException(Throwable cause, PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception: ", cause);
    }

    public PayloadRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception: " + message, cause, enableSuppression, writableStackTrace);
    }
}
