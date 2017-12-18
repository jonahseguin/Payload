package com.jonahseguin.payload.object.obj;

public interface ObjectCacheable {

    String getIdentifier();

    /**
     * Will require your class to use the Morphia @Entity, etc. annotations
     */
    boolean persist();

}
