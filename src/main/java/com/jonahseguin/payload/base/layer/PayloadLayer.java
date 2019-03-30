package com.jonahseguin.payload.base.layer;

import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;

public interface PayloadLayer<X extends Payload, D extends PayloadData> {

    X provide(D data) throws PayloadLayerCannotProvideException;



}
