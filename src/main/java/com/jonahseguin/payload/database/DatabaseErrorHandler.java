package com.jonahseguin.payload.database;

public interface DatabaseErrorHandler {

    void debug(String msg);

    void error(String msg);

    void exception(Throwable throwable, String msg);

    void exception(Throwable throwable);

}
