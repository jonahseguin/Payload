package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Data;

import java.util.UUID;

@Data
public class ProfileData implements PayloadData {

    private final String username;
    private final UUID uniqueId;
    private final String ip;

}
