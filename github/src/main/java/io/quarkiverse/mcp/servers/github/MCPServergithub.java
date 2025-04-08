package io.quarkiverse.mcp.servers.github;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.extras.HttpClientGitHubConnector;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolCallException;
import io.quarkiverse.mcp.server.WrapBusinessError;
import io.quarkus.logging.Log;

@WrapBusinessError(IOException.class)
public class MCPServergithub {

    private String githubToken;

    private HttpClientGitHubConnector gitHubConnector;

    private GitHub restClient;

    GitHub gh() throws IOException {
        assertLoggedIn();
        return restClient;
    }

    void assertLoggedIn() {
        if (githubToken == null) {
            throw new ToolCallException("GitHub token is not set. Please call login() first.");
        }
    }

    @Tool(description = "Login to GitHub. Provide your personal access token. If url is provided, please ask user to go to that url and click on the link to authorize the app.")
    String login(String token) throws IOException {
        this.githubToken = token;

        Log.info("Logging in to GitHub with token: " + token);
        this.gitHubConnector = new HttpClientGitHubConnector(
                HttpClient.newBuilder().version(Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build());

        GitHubBuilder restClientBuilder = new GitHubBuilder()
                .withConnector(gitHubConnector);
        //githubCustomizer.customize(restClientBuilder);
        restClientBuilder.withOAuthToken(githubToken);
        restClient = restClientBuilder.build();

        try {
            return "Logged in as " + restClient.getMyself().getLogin();
        } catch (IOException e) {
            this.githubToken = null;
            throw new ToolCallException("Failed to login to GitHub. Please goto https://github.com/settings/tokens");
        }

    }

    record Issue(String title, String body, String state, String createdAt, String updatedAt, String url) {
    }

    @Tool(description = "List issues in a GitHub repository with filtering options")
    List<Issue> list_issues(
            @ToolArg(description = "Owner of the repository") String owner,
            @ToolArg(description = "Name of the repository") String repo,
            @ToolArg(description = "Number of items per page", required = false) Integer perPage) throws IOException {

        var issues = gh().searchIssues().q("repo:%s/%s".formatted(owner, repo)).list();

        if (perPage == null) {
            perPage = 10;
        }

        issues.withPageSize(perPage);

        List<Issue> rawissues = new ArrayList<>();
        Iterator<GHIssue> iterator = issues.iterator();
        int count = 0;
        while (iterator.hasNext() && count < perPage) {
            GHIssue issue = iterator.next();
            rawissues.add(new Issue(issue.getTitle(), issue.getBody(), issue.getState().toString(),
                    issue.getCreatedAt().toString(), issue.getUpdatedAt().toString(), issue.getHtmlUrl().toString()));
            count++;
        }
        return rawissues;
    }

    @Tool(description = "Add a comment to an existing issue")
    String add_issue_comment(
            @ToolArg(description = "Comment body") String body,
            @ToolArg(description = "Issue number") int issue_number,
            @ToolArg(description = "Owner of the repository") String owner,
            @ToolArg(description = "Name of the repository") String repo) throws IOException {
        GHIssue issue = gh().getRepository(owner + "/" + repo).getIssue(issue_number);
        var result = issue.comment(body);
        return "Comment added: " + result.getHtmlUrl();
    }

}
