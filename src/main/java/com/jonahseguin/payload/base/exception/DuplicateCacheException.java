package com.jonahseguin.payload.base.exception;

import com.jonahseguin.payload.base.PayloadCache;

public class DuplicateCacheException extends PayloadException {

    public DuplicateCacheException(PayloadCache cache) {
        super(cache);
    }

    public DuplicateCacheException(String message, PayloadCache cache) {
        super(message, cache);
    }

    public DuplicateCacheException(String message, Throwable cause, PayloadCache cache) {
        super(message, cause, cache);
    }

    public DuplicateCacheException(Throwable cause, PayloadCache cache) {
        super(cause, cache);
    }

    public DuplicateCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, PayloadCache cache) {
        super(message, cause, enableSuppression, writableStackTrace, cache);
    }
}
