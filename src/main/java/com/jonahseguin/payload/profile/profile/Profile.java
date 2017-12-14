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
public class Profile implements ProfilePassable {

    private String name;
    private String uniqueId;

    @Transient private transient Player player = null;
    @Transient private transient boolean temporary = false;
    @Transient private transient boolean halted = false;

    public Profile() {
    }

    public Profile(String name, String uniqueId) {
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
    }

}
