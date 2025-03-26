package io.quarkiverse.mcp.servers.shared;

import java.util.List;
import java.util.function.Function;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Shared Application class for MCP CLI's.
 *
 * This class is used to run the application and handle the arguments.
 *
 * It is used to avoid code duplication between the different MCP CLI's.
 *
 * It setup a custom config source to parse arguments starting with -- or -D as a key/value pair.
 *
 * If you need to do custom stuff with the arguments, you can make your own
 *
 * @QuarkusMain class and call the SharedApplication.main method.
 *
 *              Remember to name your QuarkusMain and have `application.properties` that configures
 *              the `quarkus.package.main-class` to your QuarkusMain class.
 *
 */
@QuarkusMain
public class SharedApplication {

    public static void main(String[] args) {
        main(null, args, (remainingArgs) -> null);
    }

    public static void main(Class<? extends QuarkusApplication> app, String[] args) {
        main(app, args, (remainingArgs) -> null);
    }

    public static void main(String[] args, Function<List<String>, Void> onArgsProcessed) {
        main(null, args, onArgsProcessed);
    }

    /**
     *
     * @param app the QuarkusApplication class to run, can be null
     * @param args the arguments to pass to the application
     * @param onArgsProcessed a function that will be called with the remaining arguments after the config source has been
     *        processed
     */
    public static void main(Class<? extends QuarkusApplication> app, String[] args,
            Function<List<String>, Void> onArgsProcessed) {
        onArgsProcessed.apply(McpCliConfigSource.setupConfigSource(args));

        Quarkus.run(app,
                (exitCode, exception) -> {
                    if (exception != null) {
                        exception.printStackTrace();
                    }
                    // If we are not running in dev mode, exit the application
                    if (!LaunchMode.isDev())
                        System.exit(exitCode);
                },
                args);
    }

}
