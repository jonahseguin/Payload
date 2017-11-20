package com.jonahseguin.payload.type;

public enum CacheSource {

    USERNAME_UUID(-1),
    PRE_CACHING(0),
    NEW_PROFILE(1), // After pre-caching, but we still want them to be cached locally after
    LOCAL(2),
    REDIS(3),
    MONGO(4);

    private final int index;

    CacheSource(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public boolean hasNext() {
        return this.getIndex() < values().length && this.getIndex() != -1;
    }

    public CacheSource next() {
        for (CacheSource cacheSource : values()) {
            if (cacheSource.index == (getIndex() + 1)) {
                return cacheSource;
            }
        }
        return null;
    }

}
