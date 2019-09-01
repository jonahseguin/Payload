package com.jonahseguin.payload.mode.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class PayloadRedisProfile {

    private final String uniqueId;
    private final String username;
    private final String lastSeenServer; // Payload ID of last server they were seen at
    private final long loginTime;
    private final long lastSeen;

}
