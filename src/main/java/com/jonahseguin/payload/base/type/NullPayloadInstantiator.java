package com.jonahseguin.payload.base.type;

public class NullPayloadInstantiator<X extends Payload, D extends PayloadData> implements PayloadInstantiator<X, D> {

    @Override
    public X instantiate(D data) {
        return null;
    }
}
