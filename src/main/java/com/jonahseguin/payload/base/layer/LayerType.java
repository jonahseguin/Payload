package com.jonahseguin.payload.base.layer;

public enum LayerType {

    MAPPING, // ex Username to UUID and back
    PRE_CACHING, // ex Setting up controllers
    LOCAL, // ex local HashMap
    DATABASE_CACHE, // ex Redis HASH
    DATABASE_STORE // ex MongoDB object

}
