package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Data;

@Data
public class ProfileData implements PayloadData {

    private final String username;
    private final String uniqueId;
    private final String ip;

}
