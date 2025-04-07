# Model Context Protocol Server for Wolfram Alpha LLM API

This Model Context Protocol(MCP) server enables Large Language Models (LLMs) to perform web searches on the [Wolfram Alpha LLM API](https://products.wolframalpha.com/llm-api/documentation).

## General Usage

1. Install [jbang](https://www.jbang.dev/download/)
2. Obtain a [Wolfram Alpha App ID](https://developer.wolframalpha.com/access) to access Wolfram APIs.

Use the following command to start the server.
```shell
jbang -DWOLFRAM_LLM_API_KEY=${YOUR_WOLFRAM_APP_ID} io.quarkiverse.mcp.servers:mcp-server-wolfram:RELEASE
```

## Troubleshooting

**jbang not found**
* Make sure you have `jbang` installed and available in your PATH
* Alternatively, use full path to jbang executable (e.g. `/Users/username/.jbang/jbang`)

**Get more logging**

To get more detailed logging you can add the following parameters to the jbang command line:

```shell
-Dquarkus.log.file.enable=true -Dquarkus.log.file.path=${user.home}/mcp-server-wolfram.log
```

### How was this made?

The MCP server uses Quarkus, the Supersonic Subatomic Java Framework and its Model Context Protocol support.

If you want to learn more about Quarkus MCP Server support, please see this [blog post](https://quarkus.io/blog/mcp-server/)
and the Quarkus MCP Server [extension documentation](https://docs.quarkiverse.io/quarkus-mcp-server/dev/).

To launch the server it uses [jbang](https://jbang.dev/) to
setup Java and run the .jar as transparent as possible. Very similar to how `uvx`, `pipx`, `npmx` and others works; just for Java. 
