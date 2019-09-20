package com.jonahseguin.payload.base.exception;

import com.jonahseguin.payload.base.PayloadCache;

public class PayloadException extends Exception {

    public PayloadException(PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception");
    }

    public PayloadException(String message, PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception: " + message);
    }

    public PayloadException(String message, Throwable cause, PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception: " + message, cause);
    }

    public PayloadException(Throwable cause, PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception: ", cause);
    }

    public PayloadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, PayloadCache cache) {
        super("Payload [" + cache.getName() + "] exception: " + message, cause, enableSuppression, writableStackTrace);
    }
}
