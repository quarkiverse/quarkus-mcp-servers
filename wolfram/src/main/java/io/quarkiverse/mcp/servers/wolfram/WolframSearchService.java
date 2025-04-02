package io.quarkiverse.mcp.servers.wolfram;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkus.logging.Log;

@ApplicationScoped
public class WolframSearchService {

    static final String WOLFRAM_ALPHA_LLM_API_URL = "https://www.wolframalpha.com/api/v1/llm-api";

    @ConfigProperty(name = "wolframalpha.llm.api.appid")
    String appId;

    @Tool(description = "Perform a web search to retrieve information online with a full text response")
    String fullResponseQuery(@ToolArg(description = "Web search query") String q) {
        return doSearch(wolframSearchUrl(q));
    }

    @Tool(description = "Perform a web search to retrieve information online with a short response")
    String shortResponseQuery(@ToolArg(description = "Web search query") String q) {
        return doSearch(wolframSearchUrl(q) + "&maxchars=500");
    }

    private String wolframSearchUrl(String q) {
        return WOLFRAM_ALPHA_LLM_API_URL + "?input=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&appid=" + appId;
    }

    private static String doSearch(String webUrl) {
        String text = readWebContent(webUrl);
        Log.info("Parsed html: " + text);
        return text;
    }

    private static String readWebContent(String webUrl) {
        try {
            URL url = new URI(webUrl).toURL();
            StringBuilder sb = new StringBuilder();
            URLConnection connection = url.openConnection();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    sb.append(inputLine);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
