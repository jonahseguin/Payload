package com.jonahseguin.payload.base;

/**
 * A Payload is an object that can be cached, saved, or loaded within the payload system.  Is it the functional
 * object that we aim to handle.  Multiple implementations of a Payload are available for different caching strategies
 */
public interface Payload {

    /**
     * Gets the identifier to base our Payload on.  For local storage in a HashMap key, for instance.
     * This identifier should be unique to an object. (For example an ObjectID or UUID)
     * @return String Identifier
     */
    String getIdentifier();

}
