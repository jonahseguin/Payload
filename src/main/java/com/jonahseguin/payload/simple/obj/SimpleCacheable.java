package com.jonahseguin.payload.simple.obj;

public interface SimpleCacheable {

    String getIdentifier();

    /**
     * Will require your class to use the Morphia @Entity, etc. annotations
     */
    boolean persist();

}
