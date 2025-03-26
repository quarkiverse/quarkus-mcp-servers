package io.quarkiverse.mcp.servers.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * A shared config source for MCP CLI's
 * Will treat any argument starting with -- or -D as a key/value pair.
 *
 */
public class McpCliConfigSource implements ConfigSource {

    static List<String> setupConfigSource(String[] args) {
        List<String> remainingArgs = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith("--") || arg.startsWith("-D")) {
                String[] parts = arg.substring(2).split("=");
                if (parts.length == 2) {
                    // System.out.println("Setting " + parts[0] + " to " + parts[1]);
                    McpCliConfigSource.put(parts[0], parts[1]);
                } else {
                    McpCliConfigSource.put(parts[0], "true");
                }

                if (parts[0].equals("debug")) {
                    put("quarkus.mcp.server.client-logging.default-level", "DEBUG");
                    put("quarkus.mcp.server.traffic-logging.enabled", "true");
                    put("quarkus.log.category.\"io.quarkus.mcp.servers\".level", "DEBUG");
                }
            } else {
                remainingArgs.add(arg);
            }
        }

        boolean sse = Boolean.parseBoolean(configuration.get("sse"));

        if (sse) {
            put("quarkus.http.host-enabled", "true");
            put("quarkus.mcp.server.stdio.enabled", "false");

        } else {
            put("quarkus.http.host-enabled", "false");
            put("quarkus.mcp.server.stdio.enabled", "true");
        }

        return remainingArgs;
    }

    private static final Map<String, String> configuration = new HashMap<>();

    static {
        //   configuration.put("hass-server", "http://homeassistant.local:8123");
    }

    public static void put(String key, String value) {
        configuration.put(key, value);
    }

    @Override
    public int getOrdinal() {
        return 275;
    }

    @Override
    public Set<String> getPropertyNames() {
        return configuration.keySet();
    }

    @Override
    public String getValue(final String propertyName) {
        return configuration.get(propertyName);
    }

    @Override
    public String getName() {
        return McpCliConfigSource.class.getSimpleName();
    }
}
