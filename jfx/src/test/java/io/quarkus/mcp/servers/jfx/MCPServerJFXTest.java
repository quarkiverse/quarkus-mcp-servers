package io.quarkus.mcp.servers.jfx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;


import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import io.quarkiverse.fx.FxApplication;
import io.quarkus.runtime.Quarkus;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import javafx.application.Application;

/**
 * Basic unit test suite for the {@link MCPServerJFX} class.
 */
@QuarkusTest
public class MCPServerJFXTest {

  @TestHTTPResource
  URL url;

  @Inject ObjectMapper objectMapper;
  
  private McpClient mcpClient;

  @BeforeAll
  static void startJFX() {
    CompletableFuture.runAsync(() -> Application.launch(FxApplication.class));
  }

  @BeforeEach
  void setUpMcpClient() {


    mcpClient = new DefaultMcpClient.Builder()
      .clientName("test-mcp-client-jfx")
      .toolExecutionTimeout(Duration.ofSeconds(10))
      .transport(new HttpMcpTransport.Builder().sseUrl(url.toString() + "mcp/sse").build())
      .build();

  }

  //@Test disabling for now as getting npe on github actions
  void launchCanvas() {
    final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("launchCanvas").arguments("{}").build());
    assertThat(ret).startsWith("Canvas launched with dimensions:");
  }

}
