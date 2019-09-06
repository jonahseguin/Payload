package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Data;

@Data
public class ObjectData implements PayloadData {

    private final String identifier;

}
