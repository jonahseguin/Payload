package com.jonahseguin.payload.base.data;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.PLang;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * This class handles the local mappings of the name <--> unique object ID for each cache,
 * so that although there can be multiple PayloadCache objects in the database with the same name, they will have
 * different IDs, as mapped by the Payload plugin and stored locally in a .json file
 */
@Getter
@Setter
public class PayloadLocal {

    private String payloadID = null;
    private boolean firstStartup = true;
    private boolean debug = true;
    private YamlConfiguration config;
    private File payloadFile;

    public String getPayloadID() {
        return payloadID;
    }

    public void savePayloadID(String payloadID) {
        this.payloadID = payloadID;
        this.config.set("payload-id", this.payloadID);
        try {
            config.save(payloadFile);
        } catch (IOException ex) {
            Bukkit.getLogger().warning("[Payload] Failed to save payload-id to payload.yml");
            if (PayloadPlugin.get().isDebug()) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Load the payload id for this server instance from payload.yml
     * @return true if successful
     */
    public boolean loadPayloadID() {
        payloadFile = new File(PayloadPlugin.get().getDataFolder() + File.separator + "payload.yml");
        if (!payloadFile.exists()) {
            try {
                payloadFile.mkdirs();
                payloadFile.createNewFile();
            }
            catch (IOException ex) {
                Bukkit.getLogger().warning("[Payload] Couldn't create payload.yml");
                this.handleError();
                return false;
            }
        }

        config = new YamlConfiguration();
        try {
            config.load(payloadFile);
        }
        catch (FileNotFoundException ex) {
            Bukkit.getLogger().warning("[Payload] payload.yml doesn't exist, although we should have just created it");
            this.handleError();
            return false;
        }
        catch (IOException | InvalidConfigurationException ex) {
            Bukkit.getLogger().warning("[Payload] Failed to load payload.yml (invalid config/format or other read/write IO exception)");
            this.handleError();
            return false;
        }

        if (!config.contains("payload-id")) {
            this.payloadID = UUID.randomUUID().toString();
            this.firstStartup = true;
            config.set("payload-id", this.payloadID);
            config.set("debug", true);
            try {
                config.save(payloadFile);
            }
            catch (IOException ex) {
                Bukkit.getLogger().warning("[Payload] Failed to save new payload-id to payload.yml during write");
                this.handleError();
                return false;
            }
        }
        else {
            this.firstStartup = false;
            this.payloadID = config.getString("payload-id");
        }

        this.debug = config.getBoolean("debug", true);

        return true;
    }

    /**
     * Is this the first time the Payload plugin has loaded on this server?
     * (No payload.yml present)
     */
    public boolean isFirstStartup() {
        return this.firstStartup;
    }

    private void handleError() {
        PayloadPlugin.get().setLocked(true); // Lock server
        PayloadPlugin.get().alert(PayloadPermission.ADMIN, PLang.ERROR_FAILED_TO_LOAD_PAYLOAD_FILE); // Alert online staff if any + console
    }

}
