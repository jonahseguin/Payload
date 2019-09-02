package com.jonahseguin.payload.base.layer;

import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

public interface PayloadLayer<K, X extends Payload, D extends PayloadData> {

    X get(K key) throws PayloadLayerCannotProvideException;

    X get(D data) throws PayloadLayerCannotProvideException;

    boolean save(X payload);

    boolean has(K key);

    boolean has(D data);

    boolean has(X payload);

    void remove(K key);

    void remove(D data);

    void remove(X payload);

    int cleanup();

    long clear();

    void init();

    void shutdown();

    String layerName();

    long size();

    boolean isDatabase();

}
