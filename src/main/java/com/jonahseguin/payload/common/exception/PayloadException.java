package com.jonahseguin.payload.common.exception;

/**
 * Created by Jonah on 12/17/2017.
 * Project: Payload
 *
 * @ 3:34 PM
 */
public class PayloadException extends RuntimeException {

    public PayloadException() {
    }

    public PayloadException(String message) {
        super(message);
    }

    public PayloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public PayloadException(Throwable cause) {
        super(cause);
    }

    public PayloadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
