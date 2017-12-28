package com.jonahseguin.payload.profile.profile;

import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import org.bukkit.entity.Player;

/**
 * Created by Jonah on 11/14/2017.
 * Project: Payload
 *
 * @ 9:48 PM
 */
@Entity("profile")
@Getter
@Setter
public class PayloadProfile implements ProfilePassable {

    protected String name;
    protected String uniqueId;
    protected boolean initialized = false;

    @Transient protected transient Player player = null;
    @Transient protected transient boolean temporary = false;
    @Transient protected transient boolean halted = false;

    public PayloadProfile() {
    }

    public PayloadProfile(String name, String uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    public void initialize(Player player) {
        this.player = player;
        this.initialized = true;
    }

}
