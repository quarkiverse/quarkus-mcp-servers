package io.quarkiverse.mcp.servers.jfx;

import io.quarkiverse.fx.FxApplication;
import io.quarkiverse.mcp.servers.shared.SharedApplication;
import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import javafx.application.Application;

/**
 * Need this to ensure JFX is started early enough.
 */
@QuarkusMain(name = "jfx")
public class JFXApplication implements QuarkusApplication {

    public static void main(String[] args) {
        SharedApplication.main(JFXApplication.class, args);
    }

    @Override
    public int run(String... args) {
        // placed here as it need CDI to be available.
        System.err.println("Booting JFX run");
        Log.error("Booting JFX");
        Application.launch(FxApplication.class, args);
        return 0;
    }
}
