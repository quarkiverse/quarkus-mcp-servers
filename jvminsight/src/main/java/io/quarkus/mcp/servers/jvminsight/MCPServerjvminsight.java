package io.quarkus.mcp.servers.jvminsight;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
    
}}