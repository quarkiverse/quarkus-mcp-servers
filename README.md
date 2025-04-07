# Model Context Protocol Servers in Quarkus/Java

This project contains [Model Context Protocol](https://modelcontextprotocol.io/) servers,
implemented in Java using the [Quarkus MCP server framework](https://github.com/quarkiverse/quarkus-mcp-server).

These lets you extend the capabilities of your MCP enabled LLM (Large Language Model) AI applications.

These also work in MCP enabled LLM applications, like Claude Desktop. You can find other clients on
[Awesome MCP Clients](https://github.com/punkpeye/awesome-mcp-clients) page.

## Running the servers

All of these servers are available to run with [jbang everywhere](https://jbang.dev/everywhere): Java, JavaScript, Python and more. Where it says `jbang` it can be replaced with `npx @jbangdev/jbang` or `uvx jbang` or `pipx jbang` dependent on your preference.

## Servers

### [jdbc](jdbc)

![](jdbc/images/jdbc-trends-demo.png)

The `jdbc` server can be used to store and retrieve data from a database given just a JDBC URL. You can use any JDBC database, like Postgres, MySQL, Oracle, Sqlite, etc.

```shell
jbang jdbc@quarkiverse/quarkus-mcp-servers
```

See more in the [jdbc readme](jdbc/README.md).

### [jvminsight](jvminsight)

![](jvminsight/images/jvminsight-demo.png)

The `jvminsight` server is a server that can be used to inspect a running JVM process.

```shell
jbang jvminsight@quarkiverse/quarkus-mcp-servers
```

See more in the [jvminsight readme](jvminsight/README.md).

### [filesystem](filesystem)

The `filesystem` server is a file system server that can be used to serve files from a file system.

```shell
jbang filesystem@quarkiverse/quarkus-mcp-servers [path1] [path2] ...
```

See more in the [filesystem readme](filesystem/README.md).

### [jfx](jfx)

[![](jfx/images/jfx-demo.png)](https://www.youtube.com/watch?v=Wnh_-0dAaDI)

The `jfx` server exposes a canvas that can be used to make drawings using JavaFX.

```shell
jbang jfx@quarkiverse/quarkus-mcp-servers
```

See more in the [jfx readme](jfx/README.md).


### [kubernetes](kubernetes)

The `kubernetes` server can be used to interact with a Kubernetes cluster.

```shell
jbang kubernetes@quarkiverse/quarkus-mcp-servers
```

### [containers](containers)

The 'containers' server lets you work with Docker/Podman/OCI compatible container engines.

```shell
jbang containers@quarkiverse/quarkus-mcp-servers
```

![](containers/images/containers-demo.png)

### [wolfram](wolfram)

The `wolfram` server can be used to perform web search optimized for use by a large language model through the Wolfram Alpha LLM API.

```shell
jbang wolfram@quarkiverse/quarkus-mcp-servers
```

## Other Quarkus MCP based servers

### [WildFly](https://github.com/wildfly-extras/wildfly-mcp)

A WildFly MCP server that allows you to interact with WildFly running servers.

You can check this [WildFly vlog](https://youtu.be/wg1hAdOoe2w) that demonstrates its capabilities.

Read more in the [WildFly MCP Server readme](https://github.com/wildfly-extras/wildfly-mcp/blob/main/wildfly-mcp-server/README.md).

## Ideas for other servers

If you have ideas for other servers, feel free to contribute them to this project.

If missing ideas, then look at the reference servers at https://github.com/modelcontextprotocol/servers and see if you can implement them in this project.

Other ideas:

- zulip
- jfr/java hooked to jmx/jfr
- quarkus dev mode
- ... 

## Contributing

If you have ideas for other servers, feel free to contribute them to this project.

To get started, clone the repository and build it:

```bash
git clone https://github.com/quarkiverse/quarkus-mcp-servers
cd quarkus-mcp-servers
mvn clean install
```

Then run the following command to generate a new server for i.e. jfr:

```shell
mkdir jfr
cd jfr
jbang init -t mcp jfr
```

This will create the `jfr` directory with a Hello World MCP server.

You can then build it:

```shell
mvn clean install
```

To wire it into the full project you need to add `<module>jfr</module>` to the root `pom.xml` file.

Make sure you have added some useful content to the `README.md` file + updated the demo image.

Then open a PR :)







