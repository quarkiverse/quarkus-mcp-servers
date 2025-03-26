package io.quarkiverse.mcp.servers.kubernetes;

import static io.quarkiverse.mcp.servers.kubernetes.MCPTestUtils.initMcpStdioClient;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMixedDispatcher;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.Context;
import io.fabric8.mockwebserver.MockWebServer;
import io.fabric8.mockwebserver.ServerRequest;
import io.fabric8.mockwebserver.ServerResponse;

public class ObjectMapperCustomizerIT {

    private static KubernetesMockServer mockServer;
    private static KubernetesClient kubernetesClient;
    private static McpClient client;

    @BeforeAll
    static void setUp() throws Exception {
        final Map<ServerRequest, Queue<ServerResponse>> responses = new HashMap<>();
        mockServer = new KubernetesMockServer(new Context(new ObjectMapper()),
                new MockWebServer(), responses, new KubernetesMixedDispatcher(responses), true);
        mockServer.init();
        kubernetesClient = mockServer.createClient();
        client = initMcpStdioClient(kubernetesClient.getConfiguration().getMasterUrl());
    }

    @AfterAll
    static void closeMcpClient() {
        kubernetesClient.close();
        mockServer.destroy();
    }

    @BeforeEach
    void resetMockServer() {
        mockServer.reset();
    }

    @Test
    void serializedNamespacesDontContainManagedFields() {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces")
                .andReturn(200, new NamespaceListBuilder()
                        .addNewItem().withMetadata(new ObjectMetaBuilder()
                                .withName("a-namespace-to-list")
                                .addNewManagedField().withManager("the-manager").endManagedField()
                                .build())
                        .endItem()
                        .build())
                .once();
        assertThat(client.executeTool(ToolExecutionRequest.builder().name("namespaces_list").arguments("{}").build()))
                .isNotBlank()
                .isEqualTo("[{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"metadata\":{\"name\":\"a-namespace-to-list\"}}]");
    }

    @Test
    void serializedResourcesDontContainManagedFields() {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces")
                .andReturn(200, new NamespaceListBuilder()
                        .addNewItem().withMetadata(new ObjectMetaBuilder()
                                .withName("a-namespace-to-list")
                                .addNewManagedField().withManager("the-manager").endManagedField()
                                .build())
                        .endItem()
                        .build())
                .once();
        assertThat(client.executeTool(ToolExecutionRequest.builder().name("resources_list")
                .arguments("{\"apiVersion\":\"v1\",\"kind\":\"Namespace\"}").build()))
                .isNotBlank()
                .isEqualTo("[{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"metadata\":{\"name\":\"a-namespace-to-list\"}}]");
    }

    @Test
    void serializedResourceDoesntContainManagedFields() {
        mockServer.expect().get()
                .withPath("/api/v1/namespaces/a-namespace-to-get")
                .andReturn(200, new NamespaceBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("a-namespace-to-get")
                                .addNewManagedField().withManager("the-manager").endManagedField()
                                .build())
                        .build())
                .once();
        assertThat(client.executeTool(ToolExecutionRequest.builder().name("resources_get")
                .arguments("{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"name\":\"a-namespace-to-get\"}").build()))
                .isNotBlank()
                .isEqualTo("{\"apiVersion\":\"v1\",\"kind\":\"Namespace\",\"metadata\":{\"name\":\"a-namespace-to-get\"}}");
    }
}
