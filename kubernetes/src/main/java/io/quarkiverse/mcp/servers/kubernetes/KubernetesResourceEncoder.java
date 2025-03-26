package io.quarkiverse.mcp.servers.kubernetes;

import java.util.List;

import javax.annotation.Priority;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkiverse.mcp.server.ToolResponseEncoder;

@Singleton
@Priority(1)
public class KubernetesResourceEncoder implements ToolResponseEncoder<Object> {

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public boolean supports(Class<?> runtimeType) {
        return true;
    }

    @Override
    public ToolResponse encode(Object value) {
        return new ToolResponse(false, List.of(new TextContent(
                kubernetesClient.getKubernetesSerialization().asJson(value))));
    }
}
