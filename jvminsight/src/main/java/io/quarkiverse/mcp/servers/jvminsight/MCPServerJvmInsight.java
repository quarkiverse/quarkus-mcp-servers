package io.quarkiverse.mcp.servers.jvminsight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import io.quarkiverse.mcp.server.Prompt;
import io.quarkiverse.mcp.server.PromptMessage;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.logging.Log;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;

public class MCPServerJvmInsight {

    Map<String, VirtualMachine> vms = new HashMap<>();

    /**
     * List of paths to search for JVM binaries.
     *
     * If not set, will use java.home of the running JVM.
     */
    @ConfigProperty(name = "jvminsight.paths")
    Optional<List<String>> jvminsightPaths;

    @Tool(description = "List all running JVM processes. Result ")
    String jps() {
        // Get current process id
        String currentPid = String.valueOf(ProcessHandle.current().pid());
        Log.debugf("Current process id: %s", currentPid);

        StringBuilder sb = new StringBuilder();
        com.sun.tools.attach.VirtualMachine.list().forEach(vmd -> {
            if (vmd.id().equals(currentPid)) {
                return;
            }
            sb.append(vmd.id())
                    .append("\t")
                    .append(vmd.displayName())
                    .append("\n");
        });
        Log.infof("jps: %s", sb.toString());
        return sb.toString();
    }

    @Tool(description = "Attach to a JVM process, returns the pid of the attached process if successful")
    String attach(String pid) {
        VirtualMachine vm;
        try {
            vm = VirtualMachine.attach(pid);
        } catch (AttachNotSupportedException | IOException e) {
            throw new ToolCallException("Failed to attach to process " + pid + ": " + e.getMessage());
        }
        vms.put(pid, vm);
        return pid;
    }

    @Tool(description = "Get the system properties of a JVM process")
    Map<String, String> getSystemProperties(@ToolArg(description = "Process id to inspect") String pid) {
        VirtualMachine vm = vms.get(pid);
        if (vm == null) {
            throw new ToolCallException("Process not found. Did you forget to attach to the process?");
        }
        try {
            return vm.getSystemProperties().entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        } catch (IOException e) {
            throw new ToolCallException("Failed to get system properties for process " + pid + ": " + e.getMessage());
        }
    }

    List<Path> getJavaPaths() {
        List<Path> javaPaths = new ArrayList<>();

        Set<String> tools = new TreeSet<>();

        if (jvminsightPaths.isPresent()) {
            for (String path : jvminsightPaths.get()) {
                Path javaBin = Paths.get(path, "bin");
                if (Files.exists(javaBin)) {
                    javaPaths.add(javaBin);
                } else {
                    Log.warnf("Path %s does not exist", path);
                }
            }
        } else {
            String javaHome = System.getProperty("java.home");
            if (javaHome == null) {
                Log.warn("No java.home found - please set jvminsight.paths or java.home");
            }
            if (Files.isDirectory(Paths.get(javaHome, "bin"))) {
                javaPaths.add(Paths.get(javaHome, "bin"));
            } else {
                Log.warnf("Java bin not found in %s", javaHome);
            }
        }
        return javaPaths;
    }

    @Tool(description = "List all available tools in java.home or jvminsight.paths")
    Set<String> listAavailableTools() {
        List<Path> javaPaths = getJavaPaths();

        if (javaPaths.isEmpty()) {
            throw new ToolCallException("No java bins found - please set jvminsight.paths or java.home");
        }

        Set<String> tools = new TreeSet<>();
        for (Path javaPath : javaPaths) {
            Log.debugf("Java bin: %s", javaPath);
            try {
                Files.list(javaPath).filter(Files::isExecutable).forEach(tool -> {
                    Log.debugf("Tool: %s", tool);
                    tools.add(tool.getFileName().toString());
                });
            } catch (IOException e) {
                Log.warnf("Failed to list tools in %s: %s", javaPath, e.getMessage());
            }
        }

        Log.infof("Available tools: %s", tools);

        return tools;
    }

    @Tool(description = "Execute a Java tool or list all available tools if no tool name is provided")
    String executeJavaTool(@ToolArg(description = "Tool name to execute, or empty to list all tools") String tool,
            @ToolArg(description = "Arguments to pass to the tool") String... args) {

        List<Path> javaPaths = getJavaPaths();

        if (javaPaths.isEmpty()) {
            throw new ToolCallException("No java bins found - please set jvminsight.paths or java.home");
        }

        Path toolPath = javaPaths.stream()
                .flatMap(path -> {
                    try {
                        return Files.list(path).filter(Files::isExecutable);
                    } catch (IOException e) {
                        Log.warnf("Failed to list tools in %s: %s", path, e.getMessage());
                        return Stream.empty();
                    }
                })
                .filter(path -> path.getFileName().toString().equals(tool))
                .findFirst()
                .orElseThrow(() -> new ToolCallException("Tool " + tool + " not found"));

        StringBuilder sb = new StringBuilder();

        List<String> command = new ArrayList<>();
        command.add(toolPath.toString());
        command.addAll(Arrays.asList(args));

        ProcessExecutor executor = new ProcessExecutor()
                .command(command)
                .redirectOutput(new LogOutputStream() {
                    @Override
                    public void processLine(String line) {
                        sb.append(line).append("\n");
                    }
                })
                .redirectError(new LogOutputStream() {
                    @Override
                    public void processLine(String line) {
                        sb.append("ERROR: ").append(line).append("\n");
                    }
                });

        try {
            executor.timeout(30, TimeUnit.SECONDS).execute();
            var result = executor.execute();
            if (result.getExitValue() != 0) {
                throw new ToolCallException("Tool execution failed with exit code: " + result.getExitValue());
            }

        } catch (TimeoutException e) {
            throw new ToolCallException("Tool execution timed out after 30 seconds");
        } catch (InterruptedException | InvalidExitValueException | IOException e) {
            throw new ToolCallException("Tool execution failed: " + e.getMessage());
        }

        return sb.toString();
    }

    @Prompt(description = "Investigate and analyze a Java process running on the system")
    PromptMessage investigateJavaProcess(
            @ToolArg(description = "Process ID or descriptive name of the Java process to investigate") String processIdentifier,
            @ToolArg(description = "Optional: Specific areas to focus on (e.g., 'memory', 'threads', 'gc', 'all')", required = false) String investigationFocus) {

        @TemplateContents("""
                    # Java Process Investigation Assistant

                    You are a specialized Java diagnostics assistant. Your goal is to help identify, connect to, and analyze a Java process running on the system.
                    Below is a process ID or a description of the process I'm looking for.

                    process: {processIdentifier}

                    investigationFocus: {investigationFocus}
                    ## Process Identification

                    First, list all Java processes running on the system. If I provided:
                    - A specific PID: Verify it exists and show basic information about it
                    - A description: Help identify the likely process based on my description

                    Make sure to locate the correct process first, if in doubt, ask the user to confirm the process
                    before proceeding.

                    ## Connection and Basic Information

                    Once the correct process is identified:
                    1. Attach to the process
                    2. Retrieve and present key system properties in an organized format
                    3. Identify the Java version, classpath, and other critical environment variables

                    ## Focused Analysis

                    Based on the investigation focus I provided (or perform a general analysis if none specified):

                    - **Memory Analysis**:
                      - Use jmap to show heap usage statistics
                      - Suggest commands for analyzing memory leaks if suspected
                      - Recommend heap dump analysis if appropriate

                    - **Thread Analysis**:
                      - Use jstack to obtain thread dumps
                      - Identify blocked or high-CPU threads
                      - Analyze thread states and potential deadlocks

                    - **Performance Analysis**:
                      - Recommend appropriate JFR (Java Flight Recorder) commands
                      - Suggest JVM flags for better performance monitoring
                      - Analyze GC behavior and recommend optimizations

                    - **Application Analysis**:
                      - Identify the application framework/server (Spring, Quarkus, etc.)
                      - Locate configuration files and important application parameters
                      - Suggest application-specific diagnostic approaches

                    ## Recommendations

                    Provide actionable recommendations based on the findings:
                    - Immediate actions for critical issues
                    - Monitoring suggestions for ongoing observation
                    - Potential JVM flag changes to improve performance
                    - Further diagnostic steps if more information is needed

                    ## Step-by-Step Guide

                    If I need to perform additional diagnostics, provide me with:
                    1. Exact commands to run
                    2. How to interpret the results
                    3. Next steps based on possible findings

                    Offer user to make a visual of the relevant processs memory, threads and resource consumption.

                    Remember to use the tools available in the environment and explain your reasoning and interpretation of the results at each step.
                """)
        record prompt(String processIdentifier, String investigationFocus) implements TemplateInstance {
        }

        return PromptMessage.withUserRole(new TextContent(
                new prompt(processIdentifier, investigationFocus != null ? investigationFocus : "all").render()));
    }
}