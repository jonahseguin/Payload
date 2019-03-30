package com.jonahseguin.payload.base.exception;

import com.jonahseguin.payload.base.PayloadCache;

public class PayloadLayerCannotProvideException extends PayloadException {

    public PayloadLayerCannotProvideException(PayloadCache cache) {
        super(cache);
    }

    public PayloadLayerCannotProvideException(String message, PayloadCache cache) {
        super(message, cache);
    }

    public PayloadLayerCannotProvideException(String message, Throwable cause, PayloadCache cache) {
        super(message, cause, cache);
    }

    public PayloadLayerCannotProvideException(Throwable cause, PayloadCache cache) {
        super(cause, cache);
    }

    public PayloadLayerCannotProvideException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, PayloadCache cache) {
        super(message, cause, enableSuppression, writableStackTrace, cache);
    }
}
