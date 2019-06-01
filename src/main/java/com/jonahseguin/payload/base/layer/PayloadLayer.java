package com.jonahseguin.payload.base.layer;

import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

public interface PayloadLayer<X extends Payload, D extends PayloadData> {


    X get(D data) throws PayloadLayerCannotProvideException;

    boolean save(X payload);

    boolean has(D data);

    boolean has(X payload);

    void remove(D data);

    void remove(X payload);

    int cleanup();

    int clear();

    void init();

    void shutdown();


}
