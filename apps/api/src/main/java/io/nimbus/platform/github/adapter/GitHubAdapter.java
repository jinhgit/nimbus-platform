package io.nimbus.platform.github.adapter;

import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.provider.CreateRepositoryCommand;
import io.nimbus.platform.github.provider.CreatedRepository;
import io.nimbus.platform.github.provider.GitProvider;
import io.nimbus.platform.github.provider.GitUserProfile;
import io.nimbus.platform.github.provider.RepoFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GitHubAdapter implements GitProvider {

    private static final Logger log = LoggerFactory.getLogger(GitHubAdapter.class);
    private static final String API = "https://api.github.com";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GitHubAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(API)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .defaultHeader(HttpHeaders.USER_AGENT, "Nimbus-Platform")
                .build();
    }

    @Override
    public String providerName() {
        return "GitHub";
    }

    @Override
    public GitUserProfile validateToken(String accessToken) {
        try {
            String body = restClient.get()
                    .uri("/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            JsonNode node = objectMapper.readTree(body);
            return new GitUserProfile(
                    node.path("id").asText(),
                    node.path("login").asText(),
                    node.path("name").asText(null),
                    node.path("avatar_url").asText(null),
                    node.path("html_url").asText(null)
            );
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.GITHUB_INVALID_TOKEN, ex.getMessage());
        }
    }

    @Override
    public CreatedRepository createRepository(CreateRepositoryCommand command) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", command.repoName());
            payload.put("description", command.description() != null ? command.description() : "Created by Nimbus Platform");
            payload.put("private", command.isPrivate());
            payload.put("auto_init", true);

            String body = restClient.post()
                    .uri("/user/repos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + command.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(body);
            CreatedRepository created = new CreatedRepository(
                    node.path("id").asText(),
                    node.path("owner").path("login").asText(command.owner()),
                    node.path("name").asText(command.repoName()),
                    node.path("html_url").asText(),
                    node.path("clone_url").asText(),
                    node.path("default_branch").asText("main"),
                    node.path("private").asBoolean(command.isPrivate())
            );

            // small delay for GitHub to finish auto_init
            sleepQuiet(800);

            if (command.files() != null) {
                for (RepoFile file : command.files()) {
                    if (file == null || file.path() == null || file.path().isBlank()) {
                        continue;
                    }
                    // skip root README overwrite conflict — use explicit path
                    putFile(
                            command.accessToken(),
                            created.owner(),
                            created.name(),
                            file.path(),
                            file.content() != null ? file.content() : "",
                            "chore(nimbus): add " + file.path()
                    );
                    sleepQuiet(200);
                }
            }
            return created;
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("GitHub createRepository failed", ex);
            throw new BusinessException(ErrorCode.GITHUB_API_FAILED, ex.getMessage());
        }
    }

    @Override
    public void putFile(
            String accessToken,
            String owner,
            String repo,
            String path,
            String content,
            String message
    ) {
        try {
            String encoded = Base64.getEncoder().encodeToString(
                    content.getBytes(StandardCharsets.UTF_8)
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("message", message != null ? message : "chore(nimbus): update " + path);
            payload.put("content", encoded);
            payload.put("branch", "main");

            // if file exists, include sha
            try {
                String existing = restClient.get()
                        .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(String.class);
                JsonNode node = objectMapper.readTree(existing);
                if (node.has("sha")) {
                    payload.put("sha", node.path("sha").asText());
                }
            } catch (RestClientResponseException notFound) {
                // 404 = new file
                if (notFound.getStatusCode().value() != 404) {
                    throw notFound;
                }
            }

            restClient.put()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw mapHttpError(ex);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.GITHUB_API_FAILED, "putFile " + path + ": " + ex.getMessage());
        }
    }

    @Override
    public RateLimitStatus rateLimit(String accessToken) {
        try {
            String body = restClient.get()
                    .uri("/rate_limit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);
            JsonNode core = objectMapper.readTree(body).path("resources").path("core");
            return new RateLimitStatus(
                    core.path("remaining").asInt(0),
                    core.path("limit").asInt(0),
                    core.path("reset").asLong(0)
            );
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.GITHUB_API_FAILED, ex.getMessage());
        }
    }

    private BusinessException mapHttpError(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        String body = ex.getResponseBodyAsString();
        if (status == 401 || status == 403) {
            if (body != null && body.toLowerCase().contains("rate limit")) {
                return new BusinessException(ErrorCode.GITHUB_RATE_LIMIT);
            }
            if (status == 401) {
                return new BusinessException(ErrorCode.GITHUB_INVALID_TOKEN);
            }
            return new BusinessException(ErrorCode.GITHUB_API_FAILED, "GitHub forbidden: " + truncate(body));
        }
        if (status == 422 && body != null && body.toLowerCase().contains("name already exists")) {
            return new BusinessException(ErrorCode.GITHUB_REPO_EXISTS);
        }
        return new BusinessException(ErrorCode.GITHUB_API_FAILED, "HTTP " + status + ": " + truncate(body));
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 200 ? s.substring(0, 200) : s;
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
