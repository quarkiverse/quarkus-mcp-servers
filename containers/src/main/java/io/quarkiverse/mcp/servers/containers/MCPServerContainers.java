///usr/bin/env jbang "$0" "$@" ; exit $?
package io.quarkiverse.mcp.servers.containers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;

@ApplicationScoped
public class MCPServerContainers {

    private DockerClientConfig config;
    private DockerClient dockerClient;

    @Startup
    void init() {
        config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        Log.info("Starting Docker server with master URL: " + config.getDockerHost());
        var dockerHttpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        dockerClient = DockerClientImpl.getInstance(config, dockerHttpClient);

    }

    @Tool(description = "Get the current docker/container configuration")
    public String configuration_get() {
        return dockerClient.toString();
    }

    @Tool(description = "Get the current list of containers")
    public List<Container> containers_list() {
        return dockerClient.listContainersCmd().exec();
    }

    @Tool(description = "Get the current list of images of containers")
    public List<Image> images_list() {
        return dockerClient.listImagesCmd().exec();
    }

    @Tool(description = "Get the current list of networks of containers")
    public List<Network> networks_list() {
        return dockerClient.listNetworksCmd().exec();
    }

    @Tool(description = "Get the current list of volumes for containers")
    public ListVolumesResponse volumes_list() {
        return dockerClient.listVolumesCmd().exec();
    }

    @Tool(description = "Get logs from container")
    public List<String> container_logs(@ToolArg(description = "The name of the container") String name,
            @ToolArg(description = "The number of lines to return") int lines) {
        var logContainerCmd = dockerClient.logContainerCmd(name).withStdOut(true)
                .withStdErr(true).withTail(lines);
        List<String> logs = new ArrayList<>();
        try {
            logContainerCmd.exec(new ResultCallback.Adapter<>() {
                @Override
                public void onNext(Frame object) {
                    logs.add(object.toString());
                }
            }).awaitCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return logs;
    }

    @Prompt(description = "Service Architecture Diagram")
    PromptMessage service_architecture_diagram() {
        return PromptMessage.withUserRole(new TextContent(
                """
                        Generate a service architecture diagram showing how my containers interconnect to form complete applications.
                        """));
    }

    @Prompt(description = "Port Allocation Overview")
    PromptMessage port_allocation_overview() {
        return PromptMessage.withUserRole(new TextContent(
                """
                        Create a visualization of all port mappings across containers, highlighting exposed ports and potential conflicts.
                        """));
    }
}
