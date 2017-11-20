package com.jonahseguin.payload.profile;

import lombok.Data;

@Data
public class SimpleProfilePassable implements ProfilePassable {

    private final String uniqueId;
    private final String name;

}
