package io.quarkiverse.mcp.servers.filesystem;

import io.quarkiverse.mcp.servers.shared.SharedApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * This is the main entry point for the filesystem server.
 * It will start the server and set the fileserver.paths property based on arguments if present.
 */
@QuarkusMain(name = "filesystem")
public class FileSystemApplication {
    public static void main(String[] args) {
        SharedApplication.main(args, (remainingArgs) -> {
            if (remainingArgs.size() > 0) {
                System.setProperty("fileserver.paths", String.join(",", remainingArgs));
            }
            return null;
        });
    }
}
