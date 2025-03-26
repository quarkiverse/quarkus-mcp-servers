package io.quarkiverse.mcp.servers.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Basic unit test suite for the {@link MCPServerKubernetes} class.
 */
@QuarkusTest
@DisabledOnOs({ OS.WINDOWS, OS.MAC })
class MCPServerKubernetesTest {

    @Inject
    KubernetesClient kubernetesClient;
    @TestHTTPResource
    URL url;
    private McpClient mcpClient;

    @BeforeEach
    void setUpMcpClient() {
        mcpClient = new DefaultMcpClient.Builder()
                .clientName("test-mcp-client-kubernetes")
                .toolExecutionTimeout(Duration.ofSeconds(10))
                .transport(new HttpMcpTransport.Builder().sseUrl(url.toString() + "mcp/sse").build())
                .build();
    }

    @Test
    void configuration_get_returnsTestKubernetesMasterUrl() {
        final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("configuration_get").arguments("{}").build());
        assertThat(unmarshal(ret))
                .extracting(gkr -> gkr.get("masterUrl")).asString()
                .startsWith("https://localhost:");
    }

    @Nested
    class GenericResourceOperations {

        @Test
        void resources_list_clusterScopedWithIgnoredNamespace() {
            for (int it = 1; it <= 2; it++) {
                kubernetesClient.nodes()
                        .resource(new NodeBuilder().withMetadata(new ObjectMetaBuilder()
                                .withName("a-node-to-list-" + it)
                                .addNewManagedField().withManager("the-manager").endManagedField()
                                .build()).build())
                        .serverSideApply();
            }
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("resources_list")
                    .arguments("{\"apiVersion\":\"v1\",\"kind\":\"Node\",\"namespace\":\"ignored\"}").build());
            assertThat(unmarshalList(ret))
                    .extracting("kind", "metadata.name")
                    .contains(tuple("Node", "a-node-to-list-1"), tuple("Node", "a-node-to-list-2"));
        }

        @Test
        void resources_list_namespaceScopedAllNamespaces() {
            kubernetesClient.namespaces()
                    .resource(new NamespaceBuilder().withNewMetadata().withName("other-namespace").endMetadata().build())
                    .serverSideApply();
            kubernetesClient.configMaps()
                    .resource(new ConfigMapBuilder().withNewMetadata().withName("a-configmap-to-list").endMetadata().build())
                    .serverSideApply();
            kubernetesClient.configMaps()
                    .inNamespace("other-namespace")
                    .resource(new ConfigMapBuilder().withNewMetadata().withName("a-configmap-to-list-in-other-namespace")
                            .endMetadata().build())
                    .serverSideApply();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("resources_list")
                    .arguments("{\"apiVersion\":\"v1\",\"kind\":\"ConfigMap\"}").build());
            assertThat(unmarshalList(ret))
                    .extracting("kind", "metadata.namespace", "metadata.name")
                    .contains(
                            tuple("ConfigMap", "default", "a-configmap-to-list"),
                            tuple("ConfigMap", "other-namespace", "a-configmap-to-list-in-other-namespace"));
        }

        @Test
        void resources_get_clusterScopedWithIgnoredNamespace() {
            kubernetesClient.nodes()
                    .resource(new NodeBuilder().withNewMetadata().withName("a-node-to-get").endMetadata().build())
                    .serverSideApply();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("resources_get")
                    .arguments("{\"apiVersion\":\"v1\",\"kind\":\"Node\",\"namespace\":\"ignored\",\"name\":\"a-node-to-get\"}")
                    .build());
            assertThat(unmarshal(ret))
                    .hasFieldOrPropertyWithValue("metadata.name", "a-node-to-get");
        }

        @Test
        void resources_get_namespaceScoped() {
            kubernetesClient.configMaps()
                    .resource(new ConfigMapBuilder().withNewMetadata().withName("a-configmap-to-get").endMetadata().build())
                    .serverSideApply();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("resources_get")
                    .arguments(
                            "{\"apiVersion\":\"v1\",\"kind\":\"ConfigMap\",\"namespace\":\"default\",\"name\":\"a-configmap-to-get\"}")
                    .build());
            assertThat(unmarshal(ret))
                    .hasFieldOrPropertyWithValue("metadata.name", "a-configmap-to-get");
        }

        @Test
        void resources_create_or_update_clusterScopedWithIgnoredNamespace() {
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("resources_create_or_update")
                    .arguments(
                            "{\"resource\":\"{\\\"apiVersion\\\":\\\"v1\\\",\\\"kind\\\":\\\"Node\\\",\\\"metadata\\\":{\\\"name\\\":\\\"a-node-to-create\\\"}}\"}")
                    .build());
            assertThat(unmarshal(ret))
                    .hasFieldOrPropertyWithValue("apiVersion", "v1")
                    .hasFieldOrPropertyWithValue("kind", "Node")
                    .hasFieldOrPropertyWithValue("metadata.name", "a-node-to-create");
            assertThat(kubernetesClient.nodes().withName("a-node-to-create").get())
                    .hasFieldOrPropertyWithValue("metadata.name", "a-node-to-create");
        }

        @Test
        void resources_create_or_update_namespaceScoped() {
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("resources_create_or_update")
                    .arguments(
                            "{\"resource\":\"{\\\"apiVersion\\\":\\\"v1\\\",\\\"kind\\\":\\\"ConfigMap\\\",\\\"metadata\\\":{\\\"name\\\":\\\"a-configmap-to-create\\\"}}\"}")
                    .build());
            assertThat(unmarshal(ret))
                    .hasFieldOrPropertyWithValue("apiVersion", "v1")
                    .hasFieldOrPropertyWithValue("kind", "ConfigMap")
                    .hasFieldOrPropertyWithValue("metadata.name", "a-configmap-to-create");
            assertThat(kubernetesClient.configMaps().inNamespace("default").withName("a-configmap-to-create").get())
                    .hasFieldOrPropertyWithValue("metadata.name", "a-configmap-to-create");
        }

        @Test
        void resources_delete_clusterScopedWithIgnoredNamespace() {
            kubernetesClient.nodes()
                    .resource(new NodeBuilder().withNewMetadata().withName("a-node-to-delete").endMetadata().build())
                    .serverSideApply();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("resources_delete")
                    .arguments(
                            "{\"apiVersion\":\"v1\",\"kind\":\"Node\",\"namespace\":\"ignored\",\"name\":\"a-node-to-delete\"}")
                    .build());
            assertThat(ret)
                    .isEqualTo("Resource deleted successfully");
        }

        @Test
        void resources_delete_namespaceScoped() {
            kubernetesClient.configMaps()
                    .resource(new ConfigMapBuilder().withNewMetadata().withName("a-configmap-to-delete").endMetadata().build())
                    .serverSideApply();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("resources_delete")
                    .arguments(
                            "{\"apiVersion\":\"v1\",\"kind\":\"Node\",\"namespace\":\"default\",\"name\":\"a-configmap-to-delete\"}")
                    .build());
            assertThat(ret)
                    .isEqualTo("Resource deleted successfully");
        }
    }

    @Test
    void namespaces_list() {
        kubernetesClient.namespaces()
                .resource(new NamespaceBuilder().withNewMetadata().withName("a-namespace-to-list").endMetadata().build())
                .serverSideApply();
        final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("namespaces_list").arguments("{}").build());
        assertThat(unmarshalList(ret))
                .extracting("metadata.name")
                .contains("a-namespace-to-list");
    }

    @Nested
    class PodOperations {

        @BeforeEach
        void setRequiredServiceAccount() {
            kubernetesClient
                    .resource(new ServiceAccountBuilder().withNewMetadata().withName("default").endMetadata().build())
                    .createOr(NonDeletingOperation::update);
        }

        @Test
        void pods_list() {
            kubernetesClient.run()
                    .withName("a-pod-to-list")
                    .withImage("busybox")
                    .done();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("pods_list").arguments("{}").build());
            assertThat(unmarshalList(ret))
                    .extracting("metadata.name")
                    .contains("a-pod-to-list");
        }

        @Test
        void pods_list_in_namespace() {
            kubernetesClient.run()
                    .withName("a-pod-to-list-in-namespace")
                    .withImage("busybox")
                    .done();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("pods_list")
                    .arguments("{\"namespace\":\"default\"}").build());
            assertThat(unmarshalList(ret))
                    .extracting("metadata.name")
                    .contains("a-pod-to-list-in-namespace");
        }

        @Test
        void pods_get() {
            kubernetesClient.run()
                    .withName("a-pod-to-get")
                    .withImage("busybox")
                    .done();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("pods_get")
                    .arguments("{\"name\":\"a-pod-to-get\"}").build());
            assertThat(unmarshal(ret))
                    .extracting("metadata.name")
                    .isEqualTo("a-pod-to-get");
        }

        @Test
        void pods_delete() {
            kubernetesClient.run()
                    .withName("a-pod-to-delete")
                    .withImage("busybox")
                    .done();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("pods_delete")
                    .arguments("{\"name\":\"a-pod-to-delete\"}").build());
            assertThat(ret)
                    .isEqualTo("Pod deleted successfully");
        }

        @Test
        void pods_log() {
            kubernetesClient.run()
                    .withName("a-pod-to-log")
                    .withImage("busybox")
                    .done();
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("pods_log")
                    .arguments("{\"namespace\":\"default\",\"name\":\"a-pod-to-log\"}").build());
            assertThat(ret)
                    .isBlank();
        }

        @Test
        void pods_run_startsPod() {
            mcpClient.executeTool(ToolExecutionRequest.builder().name("pods_run")
                    .arguments("{\"namespace\":\"default\",\"name\":\"a-pod-to-run\",\"image\":\"busybox\"}").build());
            assertThat(kubernetesClient.pods().inNamespace("default").withName("a-pod-to-run")
                    .waitUntilCondition(Objects::nonNull, 10, TimeUnit.SECONDS))
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("metadata.name", "a-pod-to-run");
        }

        @Test
        void pods_run_returnsPodInfo() {
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("pods_run")
                    .arguments("{\"namespace\":\"default\",\"name\":\"a-pod-to-run-2\",\"image\":\"busybox\"}").build());
            assertThat(unmarshalList(ret))
                    .extracting("kind", "metadata.name")
                    .contains(
                            tuple("Pod", "a-pod-to-run-2"));
        }

        @Test
        void pods_run_returnsServiceInfo() {
            final var ret = mcpClient.executeTool(ToolExecutionRequest.builder().name("pods_run")
                    .arguments(
                            "{\"namespace\":\"default\",\"name\":\"a-pod-to-run-with-service\",\"image\":\"busybox\",\"port\":8080}")
                    .build());
            assertThat(unmarshalList(ret))
                    .extracting("kind", "metadata.name")
                    .contains(
                            tuple("Pod", "a-pod-to-run-with-service"),
                            tuple("Service", "a-pod-to-run-with-service"));
        }
    }

    @SuppressWarnings("unchecked")
    private List<GenericKubernetesResource> unmarshalList(String json) {
        return kubernetesClient.getKubernetesSerialization().unmarshal(json, List.class);
    }

    private GenericKubernetesResource unmarshal(String json) {
        return kubernetesClient.getKubernetesSerialization().unmarshal(json, GenericKubernetesResource.class);
    }
}
