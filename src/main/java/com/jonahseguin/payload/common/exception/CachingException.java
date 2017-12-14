package com.jonahseguin.payload.common.exception;

public class CachingException extends RuntimeException {

    public CachingException() {
    }

    public CachingException(String message) {
        super(message);
    }

    public CachingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CachingException(Throwable cause) {
        super(cause);
    }

    public CachingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
