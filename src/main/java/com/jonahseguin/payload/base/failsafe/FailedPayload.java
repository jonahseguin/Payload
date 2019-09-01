package com.jonahseguin.payload.base.failsafe;

import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Data;
import org.bukkit.entity.Player;

@Data
public class FailedPayload<X extends Payload, D extends PayloadData> {

    private final D data;
    private final long initialFailure;
    private long lastAttempt = 0;

    private Player player = null;
    private X temporaryPayload = null;

}
