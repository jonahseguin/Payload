package com.jonahseguin.payload.base.type;

public interface PayloadController<X extends Payload> {

    X cache(PayloadData data);

}
