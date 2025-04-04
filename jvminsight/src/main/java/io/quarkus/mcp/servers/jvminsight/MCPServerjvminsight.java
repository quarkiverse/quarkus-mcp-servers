package io.quarkus.mcp.servers.jvminsight;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkus.logging.Log;

public class MCPServerjvminsight {

    Map<String, VirtualMachine> vms = new HashMap<>();

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
    Properties getSystemProperties(@ToolArg(description = "Process id to inspect") String pid) {
        VirtualMachine vm = vms.get(pid);
        if (vm == null) {
            throw new ToolCallException("Process not found. Did you forget to attach to the process?");
        }
        try {
            return vm.getSystemProperties();
        } catch (IOException e) {
            throw new ToolCallException("Failed to get system properties for process " + pid + ": " + e.getMessage());
        }
    }

    @Tool(description = "List all available java tools in java bin")
    List<String> listAavailableTools() {
        List<String> tools = new ArrayList<>();
        String javaHome = System.getProperty("java.home");
        Log.debugf("Java home: %s", javaHome);

        if (javaHome == null) {
            throw new ToolCallException("Java home not found");
        }

        Path javaBin = Paths.get(javaHome, "bin");
        Log.debugf("Java bin: %s", javaBin);

        if (!Files.exists(javaBin)) {
            throw new ToolCallException("Java bin not found");
        }

        try {
            Files.list(javaBin).filter(Files::isExecutable).forEach(tool -> {
                Log.debugf("Tool: %s", tool);
                tools.add(tool.getFileName().toString());
            });
        } catch (IOException e) {
            throw new ToolCallException("Failed to list tools in java bin: " + e.getMessage());
        }

        Log.infof("Available tools: %s", tools);

        return tools;
    }

    @Tool(description = "Execute a Java tool or list all available tools if no tool name is provided")
    String executeJavaTool(@ToolArg(description = "Tool name to execute, or empty to list all tools") String tool,
            @ToolArg(description = "Arguments to pass to the tool") String... args) {

        StringBuilder sb = new StringBuilder();

        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            throw new ToolCallException("Java home not found");
        }

        Path javaBin = Paths.get(javaHome, "bin");
        if (!Files.exists(javaBin)) {
            throw new ToolCallException("Java bin not found");
        }

        Path toolPath = javaBin.resolve(tool);
        if (!Files.exists(toolPath)) {
            throw new ToolCallException("Tool " + tool + " not found in " + javaBin);
        }

        if (!Files.isExecutable(toolPath)) {
            throw new ToolCallException("Tool " + tool + " is not executable");
        }

        ProcessBuilder pb = new ProcessBuilder(toolPath.toString());
        if (args != null && args.length > 0) {
            pb.command().addAll(Arrays.asList(args));
        }

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            process.waitFor(10, TimeUnit.SECONDS);

            if (process.exitValue() != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                throw new ToolCallException("Tool execution failed with exit code " + process.exitValue());
            }
        } catch (IOException | InterruptedException e) {
            throw new ToolCallException("Failed to execute tool " + tool + ": " + e.getMessage());
        }

        return sb.toString();
    }

}
