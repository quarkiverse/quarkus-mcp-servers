package io.quarkiverse.mcp.servers.kubernetes;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;

public class MCPTestUtils {
    private MCPTestUtils() {
    }

    public static McpClient initMcpStdioClient(String masterUrl) {
        final var kubeConfigArgs = List.of(
                "-Dquarkus.kubernetes-client.api-server-url=" + masterUrl,
                "-Dquarkus.kubernetes-client.trust-certs=true",
                "-Dquarkus.kubernetes-client.namespace=test");
        final List<String> command = new ArrayList<>();
        if (Objects.equals(System.getProperty("quarkus.native.enabled"), "true")) {
            command.add(System.getProperty("native.image.path"));
            command.addAll(kubeConfigArgs);
        } else {
            command.add(ProcessHandle.current().info().command().orElseThrow());
            command.addAll(kubeConfigArgs);
            command.add("-jar");
            command.add(System.getProperty("java.jar.path"));
        }
        return new DefaultMcpClient.Builder()
                .clientName("test-mcp-client-kubernetes")
                .toolExecutionTimeout(Duration.ofSeconds(10))
                .transport(new StdioMcpTransport.Builder().command(command).logEvents(true).build())
                .build();
    }
}
