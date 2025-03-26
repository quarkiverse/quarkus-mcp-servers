///usr/bin/env jbang "$0" "$@" ; exit $?
package io.quarkiverse.mcp.servers.kubernetes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

//JAVA 17+
//DEPS io.quarkus:quarkus-bom:3.18.1@pom
//DEPS io.quarkus:quarkus-kubernetes-client
//DEPS io.quarkiverse.mcp:quarkus-mcp-server-stdio:1.0.0.Beta1
//DEPS io.fabric8:openshift-model
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteSpecBuilder;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;

@SuppressWarnings("unused")
@ApplicationScoped
public class MCPServerKubernetes {

    private static final String KUBERNETES_COMPONENT = "app.kubernetes.io/component";
    private static final String KUBERNETES_MANAGED_BY = "app.kubernetes.io/managed-by";
    private static final String KUBERNETES_NAME = "app.kubernetes.io/name";
    private static final String KUBERNETES_PART_OF = "app.kubernetes.io/part-of";
    private static final String MCP_SERVER_NAME = "mcp-server-kubernetes";
    private static final String MCP_SERVER_APP_GROUP = "mcp-kubernetes-run-sandbox";

    @Inject
    KubernetesClient kubernetesClient;

    @Startup
    void init() {
        Log.info("Starting Kubernetes server with master URL: " + kubernetesClient.getConfiguration().getMasterUrl());
    }

    @Tool(description = "Get the current Kubernetes configuration")
    public Config configuration_get() {
        try {
            return kubernetesClient.getConfiguration();
        } catch (Exception e) {
            throw new ToolCallException("Failed to get configuration: " + e.getMessage(), e);
        }
    }

    @Tool(description = "List Kubernetes resources in the current cluster by providing their apiVersion and kind and optionally the namespace")
    public Collection<GenericKubernetesResource> resources_list(
            @ToolArg(description = "apiVersion of the resources (examples of valid apiVersion are: v1, apps/v1, networking.k8s.io/v1") String apiVersion,
            @ToolArg(description = "kind of the resources (examples of valid kind are: Pod, Service, Deployment, Ingress)") String kind,
            @ToolArg(description = "Namespace to retrieve the namespaced resources from (ignored in case of cluster scoped resources)", required = false) String namespace) {
        try {
            final var resource = kubernetesClient.genericKubernetesResources(apiVersion, kind);
            if (namespace != null && !namespace.isBlank()) {
                return resource.inNamespace(namespace).list().getItems();
            }
            try {
                return resource.inAnyNamespace().list().getItems();
            } catch (Exception e) {
                return resource.list().getItems();
            }
        } catch (Exception e) {
            throw new ToolCallException("Failed to get resources for " + apiVersion + " " + kind + ": " + e.getMessage(), e);
        }
    }

    @Tool(description = "Get a Kubernetes resource in the current cluster by providing its apiVersion, kind, optionally the namespace, and its name")
    public GenericKubernetesResource resources_get(
            @ToolArg(description = "apiVersion of the resources (examples of valid apiVersion are: v1, apps/v1, networking.k8s.io/v1") String apiVersion,
            @ToolArg(description = "kind of the resources (examples of valid kind are: Pod, Service, Deployment, Ingress)") String kind,
            @ToolArg(description = "Namespace to retrieve the namespaced resource from (ignored in case of cluster scoped resources)", required = false) String namespace,
            @ToolArg(description = "Name of the resource", required = false) String name) {
        try {
            return kubernetesClient.genericKubernetesResources(apiVersion, kind)
                    .inNamespace(namespace == null ? kubernetesClient.getNamespace() : namespace)
                    .withName(name)
                    .get();
        } catch (Exception e) {
            throw new ToolCallException("Failed to get the resource for " + apiVersion + " " + kind + ": " + e.getMessage(), e);
        }
    }

    @Tool(description = "Create or update a Kubernetes resource in the current cluster by providing a YAML or JSON representation of the resource")
    public HasMetadata resources_create_or_update(
            @ToolArg(description = "A JSON or YAML containing a representation of the Kubernetes resource. Should include top-level fields such as apiVersion,kind,metadata, and spec") String resource) {
        try {
            return kubernetesClient.resource(resource).createOr(NonDeletingOperation::update);
        } catch (Exception e) {
            throw new ToolCallException("Failed to create or update the resource: " + e.getMessage(), e);
        }
    }

    @Tool(description = "Delete a Kubernetes resource in the current cluster by providing its apiVersion, kind, optionally the namespace, and its name")
    public String resources_delete(
            @ToolArg(description = "apiVersion of the resources (examples of valid apiVersion are: v1, apps/v1, networking.k8s.io/v1") String apiVersion,
            @ToolArg(description = "kind of the resources (examples of valid kind are: Pod, Service, Deployment, Ingress)") String kind,
            @ToolArg(description = "Namespace to retrieve the namespaced resource from (ignored in case of cluster scoped resources)", required = false) String namespace,
            @ToolArg(description = "Name of the resource", required = false) String name) {
        try {
            kubernetesClient.genericKubernetesResources(apiVersion, kind)
                    .inNamespace(namespace == null ? kubernetesClient.getNamespace() : namespace)
                    .withName(name)
                    .withTimeout(10, TimeUnit.SECONDS)
                    .delete();
            return "Resource deleted successfully";
        } catch (Exception e) {
            throw new ToolCallException("Failed to delete the resource for " + apiVersion + " " + kind + ": " + e.getMessage(),
                    e);
        }
    }

    @Tool(description = "List all the Kubernetes namespaces in the current cluster")
    public Collection<Namespace> namespaces_list() {
        try {
            return kubernetesClient.namespaces().list().getItems();
        } catch (Exception e) {
            throw new ToolCallException("Failed to list namespaces: " + e.getMessage(), e);
        }
    }

    @Tool(description = "List all the Kubernetes pods in the current cluster")
    public Collection<Pod> pods_list() {
        try {
            return kubernetesClient.pods().inAnyNamespace().list().getItems();
        } catch (Exception e) {
            try {
                return kubernetesClient.pods().list().getItems();
            } catch (Exception e2) {
                throw new ToolCallException("Failed to list pods: " + e2.getMessage(), e2);
            }
        }
    }

    @Tool(description = "List all the Kubernetes pods in the specified namespace in the current cluster")
    public Collection<Pod> pods_list_in_namespace(@ToolArg(description = "Namespace to list pods from") String namespace) {
        try {
            return kubernetesClient.pods().inNamespace(namespace).list().getItems();
        } catch (Exception e) {
            throw new ToolCallException("Failed to list pods in namespace: " + e.getMessage(), e);
        }
    }

    @Tool(description = "Get a Kubernetes Pod in the current namespace with the provided name")
    public Pod pods_get(
            @ToolArg(description = "Namespace to get the Pod from", required = false) String namespace,
            @ToolArg(description = "Name of the Pod", required = false) String name) {
        try {
            return kubernetesClient.pods()
                    .inNamespace(namespace == null ? kubernetesClient.getNamespace() : namespace)
                    .withName(name)
                    .get();
        } catch (Exception e) {
            throw new ToolCallException("Failed to get pod: " + e.getMessage(), e);
        }
    }

    @Tool(description = "Delete a Kubernetes Pod in the current namespace with the provided name")
    public String pods_delete(
            @ToolArg(description = "Namespace to delete the Pod from", required = false) String namespace,
            @ToolArg(description = "Name of the Pod to delete") String name) {
        try {
            final var effectiveNamespace = namespace == null ? kubernetesClient.getNamespace() : namespace;
            final var podResource = kubernetesClient.pods()
                    .inNamespace(effectiveNamespace)
                    .withName(name);
            final var currentPod = podResource.get();
            final var isManaged = currentPod != null && currentPod.getMetadata().getLabels()
                    .getOrDefault(KUBERNETES_MANAGED_BY, "").equals(MCP_SERVER_NAME);
            if (isManaged) {
                kubernetesClient.services().inNamespace(effectiveNamespace)
                        .withLabel(KUBERNETES_MANAGED_BY, MCP_SERVER_NAME)
                        .withLabel(KUBERNETES_NAME, currentPod.getMetadata().getLabels().get(KUBERNETES_NAME))
                        .withTimeout(10, TimeUnit.SECONDS)
                        .delete();
            }
            if (isManaged && kubernetesClient.supports(Route.class)) {
                kubernetesClient.resources(Route.class).inNamespace(effectiveNamespace)
                        .withLabel(KUBERNETES_MANAGED_BY, MCP_SERVER_NAME)
                        .withLabel(KUBERNETES_NAME, currentPod.getMetadata().getLabels().get(KUBERNETES_NAME))
                        .withTimeout(10, TimeUnit.SECONDS)
                        .delete();
            }
            podResource.withTimeout(10, TimeUnit.SECONDS).delete();
        } catch (Exception e) {
            throw new ToolCallException("Failed to delete Pod: " + e.getMessage(), e);
        }
        return "Pod deleted successfully";
    }

    @Tool(description = "Get the logs of a Kubernetes Pod in the current namespace with the provided name")
    public String pods_log(
            @ToolArg(description = "Namespace to get the Pod from", required = false) String namespace,
            @ToolArg(description = "Name of the Pod", required = false) String name) {
        try {
            return kubernetesClient.pods()
                    .inNamespace(namespace == null ? kubernetesClient.getNamespace() : namespace)
                    .withName(name)
                    .limitBytes(512)
                    .getLog();
        } catch (Exception e) {
            throw new ToolCallException("Failed to get logs for pod: " + e.getMessage(), e);
        }
    }

    @Tool(description = "Run a Kubernetes Pod in the current namespace with the provided container image and optional name")
    public Collection<HasMetadata> pods_run(
            @ToolArg(description = "Namespace to run the Pod in", required = false) String namespace,
            @ToolArg(description = "Name of the Pod (Optional, random name if not provided)", required = false) String name,
            @ToolArg(description = "Container Image to run in the Pod") String image,
            @ToolArg(description = "TCP/IP port to expose from the Pod container (Optional, no port exposed if not provided)", required = false) Integer port) {
        try {
            final Collection<HasMetadata> createdResources = new ArrayList<>();
            final var effectiveName = name == null ? "mcp-kubernetes-pod-" + System.currentTimeMillis() : name;
            final var effectiveNamespace = namespace == null ? kubernetesClient.getNamespace() : namespace;
            final var labels = Map.of(
                    KUBERNETES_NAME, effectiveName,
                    KUBERNETES_COMPONENT, effectiveName,
                    KUBERNETES_MANAGED_BY, MCP_SERVER_NAME,
                    KUBERNETES_PART_OF, MCP_SERVER_APP_GROUP);
            final var runCommand = kubernetesClient.run()
                    .inNamespace(effectiveNamespace)
                    .withName(effectiveName)
                    .withImage(image)
                    .withNewRunConfig()
                    .addToLabels(labels);
            if (port != null) {
                runCommand.withPort(port);
                final var service = new ServiceBuilder()
                        .withNewMetadata().withName(effectiveName).withNamespace(effectiveNamespace).withLabels(labels)
                        .endMetadata()
                        .withSpec(new ServiceSpecBuilder()
                                .addToSelector(labels)
                                .withType("ClusterIP")
                                .addNewPort().withPort(port).withNewTargetPort().withValue(port).endTargetPort()
                                .withProtocol("TCP").endPort()
                                .build())
                        .build();
                createdResources.add(kubernetesClient.resource(service).unlock().createOr(NonDeletingOperation::update));
            }
            if (port != null && kubernetesClient.supports(Route.class)) {
                final var route = new RouteBuilder()
                        .withNewMetadata().withName(effectiveName).withNamespace(effectiveNamespace).withLabels(labels)
                        .endMetadata()
                        .withSpec(new RouteSpecBuilder()
                                .withNewTo().withKind("Service").withName(effectiveName).withWeight(100).endTo()
                                .withNewPort().withNewTargetPort().withValue(port).endTargetPort().endPort()
                                .withNewTls().withTermination("edge").withInsecureEdgeTerminationPolicy("Redirect").endTls()
                                .build())
                        .build();
                createdResources.add(kubernetesClient.resource(route).unlock().createOr(NonDeletingOperation::update));
            }
            createdResources.add(runCommand.done());
            return createdResources;
        } catch (Exception e) {
            throw new ToolCallException("Failed to run pod: " + e.getMessage(), e);
        }
    }
}
