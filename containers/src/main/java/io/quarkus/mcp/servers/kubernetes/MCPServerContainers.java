///usr/bin/env jbang "$0" "$@" ; exit $?
package io.quarkus.mcp.servers.containers;
//JAVA 17+
//DEPS io.quarkus:quarkus-bom:3.18.1@pom
//DEPS io.quarkus:quarkus-kubernetes-client
//DEPS io.quarkiverse.mcp:quarkus-mcp-server-stdio:1.0.0.Beta1
//DEPS io.fabric8:openshift-model

import java.time.Duration;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import io.quarkiverse.mcp.server.Tool;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;

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

  @Tool(description = "Get the current docker configuration")
  public String configuration_get() {
    return dockerClient.toString();
  }

  @Tool(description = "Get the current list of containers")
  public List<Container> containers_list() {
    return dockerClient.listContainersCmd().exec();
  }

  @Tool(description = "Get the current list of images")
  public List<Image> images_list() {
    return dockerClient.listImagesCmd().exec();
  }

  @Tool(description = "Get the current list of networks")
  public List<Network> networks_list() {
    return dockerClient.listNetworksCmd().exec();
  }


  @Tool(description = "Get the current list of volumes")
  public ListVolumesResponse volumes_list() {
    return dockerClient.listVolumesCmd().exec();
  }
}
