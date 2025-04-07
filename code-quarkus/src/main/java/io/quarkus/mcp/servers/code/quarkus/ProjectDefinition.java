package io.quarkus.mcp.servers.code.quarkus;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectDefinition(
        String groupId,
        String artifactId,
        String version,
        String className,
        String path,
        String buildTool,
        Integer javaVersion,
        Boolean noCode,
        Boolean noExamples,
        Set<String> extensions) {

    private static final String DEFAULT_GROUPID = "org.acme";
    private static final String DEFAULT_ARTIFACTID = "code-with-quarkus";
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";
    private static final String DEFAULT_BUILDTOOL = "MAVEN";
    private static final Boolean DEFAULT_NO_CODE = false;

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String groupId = DEFAULT_GROUPID;
        private String artifactId = DEFAULT_ARTIFACTID;
        private String version = DEFAULT_VERSION;
        private String className = null;
        private String path = null;
        private String buildTool = DEFAULT_BUILDTOOL;
        private Integer javaVersion = null;
        private Boolean noCode = DEFAULT_NO_CODE;
        private Boolean noExamples = DEFAULT_NO_CODE;
        private Set<String> extensions = Set.of();

        private Builder() {
        }

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder buildTool(String buildTool) {
            this.buildTool = buildTool;
            return this;
        }

        public Builder javaVersion(Integer javaVersion) {
            this.javaVersion = javaVersion;
            return this;
        }

        public Builder noCode(Boolean noCode) {
            this.noCode = noCode;
            return this;
        }

        public Builder noExamples(Boolean noExamples) {
            this.noExamples = noExamples;
            return this;
        }

        public Builder extensions(Set<String> extensions) {
            this.extensions = extensions;
            return this;
        }

        public ProjectDefinition build() {
            return new ProjectDefinition(groupId, artifactId, version, className, path, buildTool, javaVersion,
                    noCode, noExamples, extensions);
        }
    }

}
