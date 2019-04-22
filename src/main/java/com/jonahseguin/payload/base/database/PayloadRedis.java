package com.jonahseguin.payload.base.database;

import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;

@Data
public class PayloadRedis {

    private final String address;
    private final int port;

    private final boolean auth;
    private final String password;

    private final String uri;

    public static PayloadRedis fromConfig(ConfigurationSection section) {
        String address = section.getString("address");
        int port = section.getInt("port");

        ConfigurationSection authSection = section.getConfigurationSection("auth");
        boolean auth = authSection.getBoolean("enabled");
        String password = authSection.getString("password");

        String uri = section.getString("uri", null); // Default uri to null
        // The connection URI, if provided, will completely overwrite all other properties.

        return new PayloadRedis(address, port, auth, password, uri);
    }

    public boolean useURI() {
        return this.uri != null;
    }

}
