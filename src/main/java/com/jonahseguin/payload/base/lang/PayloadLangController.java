package com.jonahseguin.payload.base.lang;

import com.jonahseguin.payload.PayloadPlugin;
import org.apache.commons.lang.Validate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The PayloadLangController controls all language-related contents that can be customized within the Payload plugin.
 * The goal of Payload and the language controller is that every message can be customized to your specification.
 *
 * To implement your messages, simply use the {@link #injectDefinitions(Map)} method or the {@link #injectDefinition(PLang, String)}} and the default values will be replaced
 * for this instance of PayloadLangController.
 *
 * It is important to note that each PayloadCache instance has it's own language controller, and thus the language definitions
 * need to be injected for each one.  This also allows for different language definitions for different plugins implementing caches
 * and for multiple caches or cache types that you may wish to provide different messages or message styles for.
 *
 * ChatColor and "{0} {1} {2}" etc. formatting are supported for arguments.
 */
public class PayloadLangController {

    private final ConcurrentMap<PLang, String> definitions = new ConcurrentHashMap<>();

    public PayloadLangController() {
        this.createDefaults();
    }

    /**
     * Create default definitions based on the PLang enumeration's default 'text' value for each key
     */
    private void createDefaults() {
        for (PLang lang : PLang.values()) {
            this.definitions.put(lang, lang.get());
        }
    }

    /**
     * Copy & inject language definitions from an external source into this Language Controller
     * @param yourDefinitions Definitions to inject
     */
    public void injectDefinitions(Map<PLang, String> yourDefinitions) {
        Validate.notNull(yourDefinitions, "Definitions must not be null");
        Validate.notEmpty(yourDefinitions, "Definitions must not be empty");

        this.definitions.putAll(yourDefinitions);
    }

    /**
     * Copy and inject a single language definition into this Language Controller
     * @param key the Language Definition: {@link PLang} enum key for the definition to replace
     * @param definition The new definition (with formatting & arguments)
     */
    public void injectDefinition(PLang key, String definition) {
        Validate.notNull(key, "PLang must not be null");
        Validate.notNull(definition, "Definition must not be null");
        this.definitions.put(key, definition);
    }

    /**
     * Get a raw unformatted definition for a specified key, from this controller's map of language definitions
     * @param key The {@link PLang} key
     * @return The raw definition
     */
    public String getRawDefinition(PLang key) {
        Validate.notNull(key, "PLang must not be null");
        return this.definitions.getOrDefault(key, null);
    }

    /**
     * Get a formatted Language Definition based on a key and arguments, from this language controller's map of
     * language definitions.
     * @param key The {@link PLang} key
     * @param args (optional) Array of arguments to be passed for formatting the definition, in order from index {0} upwards.
     * @return The formatted text: with ChatColor and custom language / arguments
     */
    public String get(PLang key, String... args) {
        Validate.notNull(key, "PLang must not be null");
        return PayloadPlugin.format(this.getRawDefinition(key), args);
    }

}
