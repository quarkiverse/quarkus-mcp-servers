# Model Context Protocol Server for jvminsight

This Model Context Protocol(MCP) server enables Large Language Models (LLMs) to get insights into JVM applications.

This jvminsights lets you attach to jvm processes on your local machine and get info about it.

Also lets you execute tools found in `bin` folder of `java.home`.

## General Usage 

1. Install [jbang](https://www.jbang.dev/download/)
2. Configure your MCP Client to run the server (see [Claude Desktop Config](#claude-desktop-config) below)

Below are examples of command lines to use for configuring the server.

Start server:

```shell
jbang jvminsight@quarkiverse/quarkus-mcp-servers
```

## Components

Below are the MCP components provided by this server.

### Tools 

* **** - say hello to the user

### Prompts

* **make_greeting** - example prompt to get started exploring the server

## Claude Desktop Config and [mcp-cli](https://github.com/chrishayuk/mcp-cli)

Add this to your `claude_desktop.json` or `server_config.json` file:

```json
{
  "mcpServers": {
    "jdbc": {
      "command": "jbang",
      "args": [
        "mcp-server-jvminsight@quarkiverse/quarkus-mcp-servers"
      ]
    }
  }
}
```
