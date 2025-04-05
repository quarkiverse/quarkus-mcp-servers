package io.quarkus.mcp.servers.code.quarkus;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;

public class Tools {

    private final ObjectMapper mapper;

    @Inject
    public Tools(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Tool(description = "Returns a cURL command that can then be executed in the command line in order to generate a new Quarkus project")
    public String createCommand(@ToolArg(description = "The groupId of the generated project", required = false) String groupId,
            @ToolArg(description = "The artifactId of the generated project", required = false) String artifactId,
            @ToolArg(description = "Build tool of the generated project. Allowed values are 'MAVEN', 'GRADLE' or 'GRADLE_KOTLIN_DSL'", required = false) String buildTool)
            throws JsonProcessingException {
        ProjectDefinition.Builder builder = ProjectDefinition.builder();
        if (groupId != null) {
            builder.groupId(groupId);
        }
        if (artifactId != null) {
            builder.artifactId(artifactId);
        }
        if (buildTool != null) {
            builder.buildTool(buildTool);
        }
        String body = mapper.writeValueAsString(builder.build());

        return "curl -sS -X 'POST' 'https://code.quarkus.io/api/download' -H 'Content-Type: application/json' -o code-with-quarkus.zip -d '"
                + body + "'";
    }
}
