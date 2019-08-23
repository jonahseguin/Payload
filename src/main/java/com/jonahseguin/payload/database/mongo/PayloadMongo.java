package com.jonahseguin.payload.database.mongo;

import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;

@Data
public class PayloadMongo {

    private final String address;
    private final int port;
    private final String database;

    private final boolean auth;
    private final String authDatabase;
    private final String username;
    private final String password;

    private final String uri;

    public static PayloadMongo fromConfig(ConfigurationSection section) {
        String address = section.getString("address");
        int port = section.getInt("port");
        String database = section.getString("database");

        ConfigurationSection authSection = section.getConfigurationSection("auth");
        boolean auth = authSection.getBoolean("enabled");
        String authDatabase = authSection.getString("authDatabase");
        String username = authSection.getString("username");
        String password = authSection.getString("password");

        String uri = section.getString("uri", null); // Default uri to null
        // The connection URI, if provided, will completely overwrite all other properties.

        return new PayloadMongo(address, port, database, auth, authDatabase, username, password, uri);
    }

    public boolean useURI() {
        return this.uri != null;
    }

}
