package com.jonahseguin.payload.base;

@FunctionalInterface
public interface PayloadCallback<T> {

    /**
     * Called when the task is completed
     * @param object the callback object provided when the task is completed
     */
    void callback(T object);

}
