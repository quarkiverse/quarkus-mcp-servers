package io.quarkiverse.mcp.servers.filesystem;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.logging.Log;

@ApplicationScoped
public class FSUtil {

    private List<String> allowedPaths;

    public FSUtil(@ConfigProperty(name = "fileserver.paths") List<String> allowedPaths) {
        this.allowedPaths = allowedPaths.stream().map(FSUtil::expandHome).collect(Collectors.toList());

    }

    public static String expandHome(String filepath) {
        if (filepath.startsWith("~/") || filepath.equals("~")) {
            Log.info("Expanding home path: " + filepath);
            return Path.of(System.getProperty("user.home"), filepath.substring(1))
                    .toString();
        }
        return filepath;
    }

    Path validateAndResolvePath(String path) {
        path = expandHome(path);

        Path resolvedPath = Path.of(path).normalize();
        Log.info("Resolved path: " + resolvedPath);
        if (!allowedPaths.stream().anyMatch(allowedPath -> resolvedPath.startsWith(Path.of(allowedPath).normalize()))) {
            throw new ToolCallException("Access denied: Path '" + path + "' is not within allowed directories", null);
        }
        return resolvedPath;
    }

    Path validatePath(String requestedPath) throws IOException {
        String expandedPath = expandHome(requestedPath);

        // resolve relative paths to current working dir if need bePat
        Path absolute = Path.of(expandedPath).isAbsolute() ? Path.of(expandedPath)
                : Path.of(System.getProperty("user.dir")).resolve(expandedPath);

        Path normalizedRequested = absolute.normalize();

        // Check if path is within allowed directories
        boolean isAllowed = allowedPaths.stream()
                .map(dir -> Path.of(dir).normalize())
                .anyMatch(dir -> normalizedRequested.startsWith(dir));

        if (!isAllowed) {
            throw new ToolCallException(
                    String.format("Access denied - path outside allowed directories: %s not in %s",
                            absolute, String.join(", ", allowedPaths)),
                    null);
        }

        try {
            // Handle symlinks by checking their real path
            Path realPath = absolute.toRealPath();
            Path normalizedReal = realPath.normalize();

            boolean isRealPathAllowed = allowedPaths.stream()
                    .map(dir -> Path.of(dir).normalize())
                    .anyMatch(dir -> normalizedReal.startsWith(dir));

            if (!isRealPathAllowed) {
                throw new ToolCallException("Access denied - symlink target outside allowed directories", null);
            }
            return realPath;

        } catch (IOException e) {
            // For new files that don't exist yet, verify parent directory
            Path parentDir = absolute.getParent();
            try {
                Path realParentPath = parentDir.toRealPath();
                Path normalizedParent = realParentPath.normalize();

                boolean isParentAllowed = allowedPaths.stream()
                        .map(dir -> Path.of(dir).normalize())
                        .anyMatch(dir -> normalizedParent.startsWith(dir));

                if (!isParentAllowed) {
                    throw new ToolCallException("Access denied - parent directory outside allowed directories",
                            null);
                }
                return absolute;

            } catch (IOException ex) {
                throw new ToolCallException("Parent directory does not exist: " + parentDir, ex);
            }
        }
    }

    public List<String> getAllowedPaths() {
        return allowedPaths;
    }
}
