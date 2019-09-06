package com.jonahseguin.payload.base.type;

import org.mongodb.morphia.query.Query;

public interface PayloadQueryModifier<X extends Payload> {

    void apply(Query<X> query);

}
