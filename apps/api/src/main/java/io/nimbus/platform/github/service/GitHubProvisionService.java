package io.nimbus.platform.github.service;

import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.github.domain.GitHubConnection;
import io.nimbus.platform.github.domain.GitRepositoryRecord;
import io.nimbus.platform.github.domain.RepoStatus;
import io.nimbus.platform.github.dto.GitHubDtos;
import io.nimbus.platform.github.provider.CreateRepositoryCommand;
import io.nimbus.platform.github.provider.CreatedRepository;
import io.nimbus.platform.github.provider.GitProvider;
import io.nimbus.platform.github.provider.RepoFile;
import io.nimbus.platform.github.repository.GitRepositoryRecordRepository;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import io.nimbus.platform.wizard.dto.WizardDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class GitHubProvisionService {

    private final GitHubConnectionService connectionService;
    private final GitRepositoryRecordRepository repositoryRecordRepository;
    private final GitProvider gitProvider;
    private final ObjectMapper objectMapper;

    public GitHubProvisionService(
            GitHubConnectionService connectionService,
            GitRepositoryRecordRepository repositoryRecordRepository,
            GitProvider gitProvider,
            ObjectMapper objectMapper
    ) {
        this.connectionService = connectionService;
        this.repositoryRecordRepository = repositoryRecordRepository;
        this.gitProvider = gitProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Wizard Preview 기반 실제 GitHub Repository 생성.
     */
    @Transactional
    public GitRepositoryRecord provisionFromWizard(ServiceWizard wizard, WizardDtos.PreviewResponse preview) {
        GitHubConnection connection = connectionService.findActiveEntity(wizard.getCreatedBy())
                .orElseThrow(() -> new BusinessException(ErrorCode.GITHUB_NOT_CONNECTED));

        String token = connectionService.decryptToken(connection);
        String repoName = sanitizeRepoName(wizard.getServiceName());

        if (repositoryRecordRepository.existsByOwnerAndRepoNameAndDeletedAtIsNull(connection.getLogin(), repoName)) {
            throw new BusinessException(ErrorCode.GITHUB_REPO_EXISTS);
        }

        WizardDtos.PreviewResponse effectivePreview = preview != null ? preview : parsePreview(wizard);
        List<RepoFile> files = buildFiles(wizard, effectivePreview);

        GitRepositoryRecord record = GitRepositoryRecord.create(
                connection.getId(),
                wizard.getCreatedBy(),
                wizard.getWorkspaceId(),
                wizard.getProjectId(),
                wizard.getId(),
                connection.getLogin(),
                repoName,
                "PRIVATE"
        );
        record = repositoryRecordRepository.save(record);

        try {
            CreatedRepository created = gitProvider.createRepository(new CreateRepositoryCommand(
                    token,
                    connection.getLogin(),
                    repoName,
                    "Nimbus provisioned service: " + wizard.getServiceName(),
                    true,
                    files
            ));
            record.markCreated(
                    created.githubRepoId(),
                    created.htmlUrl(),
                    created.cloneUrl(),
                    created.defaultBranch()
            );
            record.setStatus(RepoStatus.WORKFLOW_GENERATED);
            record.markReady();
            return repositoryRecordRepository.save(record);
        } catch (RuntimeException ex) {
            record.markFailed();
            repositoryRecordRepository.save(record);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<GitHubDtos.RepositoryResponse> listRepos(UUID userId) {
        return repositoryRecordRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isConnected(UUID userId) {
        return connectionService.findActiveEntity(userId).isPresent();
    }

    private WizardDtos.PreviewResponse parsePreview(ServiceWizard wizard) {
        if (wizard.getPreviewJson() != null && !wizard.getPreviewJson().isBlank()) {
            try {
                return objectMapper.readValue(wizard.getPreviewJson(), WizardDtos.PreviewResponse.class);
            } catch (Exception ignored) {
                // fall through
            }
        }
        return null;
    }

    private List<RepoFile> buildFiles(ServiceWizard wizard, WizardDtos.PreviewResponse preview) {
        List<RepoFile> files = new ArrayList<>();
        String name = wizard.getServiceName();
        String runtime = wizard.getRuntime() != null ? wizard.getRuntime().name() : "UNKNOWN";

        files.add(new RepoFile("README.md", """
                # %s

                Provisioned by **Nimbus Platform** Service Wizard.

                - Runtime: `%s`
                - Environment: `%s`
                - Database: `%s`
                - Cache: `%s`

                ## Structure

                ```
                src/
                helm/
                terraform/
                argocd/
                .github/workflows/
                ```

                > Free-only MVP: files generated via GitHub API.
                """.formatted(
                name,
                runtime,
                wizard.getEnvironmentType(),
                wizard.getDatabaseType(),
                wizard.getCacheType()
        )));

        files.add(new RepoFile("Dockerfile", dockerfileFor(runtime)));
        files.add(new RepoFile(".gitignore", """
                target/
                build/
                .gradle/
                node_modules/
                .env
                .idea/
                *.iml
                .DS_Store
                """));

        if (preview != null) {
            if (preview.helmValues() != null) {
                files.add(new RepoFile("helm/values.yaml", preview.helmValues()));
                files.add(new RepoFile("helm/Chart.yaml", """
                        apiVersion: v2
                        name: %s
                        description: Nimbus generated Helm chart
                        type: application
                        version: 0.1.0
                        appVersion: "1.0.0"
                        """.formatted(name)));
            }
            if (preview.terraformVars() != null) {
                files.add(new RepoFile("terraform/terraform.tfvars", preview.terraformVars()));
                files.add(new RepoFile("terraform/main.tf", """
                        # Generated by Nimbus (free-only: local apply optional)
                        variable "project" { type = string }
                        variable "environment" { type = string }
                        variable "replica" { type = number }
                        output "project" { value = var.project }
                        """));
            }
            if (preview.githubActions() != null) {
                files.add(new RepoFile(".github/workflows/ci-cd.yml", preview.githubActions()));
            }
            if (preview.deploymentYaml() != null) {
                files.add(new RepoFile("k8s/deployment.yaml", preview.deploymentYaml()));
            }
            if (preview.argoApplication() != null) {
                files.add(new RepoFile("argocd/application.yaml", preview.argoApplication()));
            }
            if (preview.blueprint() != null) {
                files.add(new RepoFile("docs/blueprint.yaml", preview.blueprint()));
            }
        }

        files.add(new RepoFile("src/README.md", "Application source placeholder — generated by Nimbus."));
        return files;
    }

    private static String dockerfileFor(String runtime) {
        return switch (runtime) {
            case "SPRING_BOOT" -> """
                    FROM eclipse-temurin:21-jre
                    WORKDIR /app
                    COPY build/libs/*.jar app.jar
                    EXPOSE 8080
                    ENTRYPOINT ["java","-jar","/app/app.jar"]
                    """;
            case "NEXTJS", "NESTJS", "NODEJS" -> """
                    FROM node:22-alpine
                    WORKDIR /app
                    COPY package*.json ./
                    RUN npm ci --omit=dev
                    COPY . .
                    EXPOSE 3000
                    CMD ["npm","start"]
                    """;
            case "FASTAPI", "PYTHON" -> """
                    FROM python:3.13-slim
                    WORKDIR /app
                    COPY requirements.txt .
                    RUN pip install --no-cache-dir -r requirements.txt
                    COPY . .
                    EXPOSE 8000
                    CMD ["uvicorn","main:app","--host","0.0.0.0","--port","8000"]
                    """;
            default -> """
                    FROM alpine:3.20
                    WORKDIR /app
                    COPY . .
                    CMD ["echo","Nimbus generated image"]
                    """;
        };
    }

    private static String sanitizeRepoName(String name) {
        String slug = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "nimbus-service";
        }
        if (slug.length() > 100) {
            slug = slug.substring(0, 100);
        }
        return slug;
    }

    private GitHubDtos.RepositoryResponse toResponse(GitRepositoryRecord r) {
        return new GitHubDtos.RepositoryResponse(
                r.getId(),
                r.getOwner(),
                r.getRepoName(),
                r.getHtmlUrl(),
                r.getCloneUrl(),
                r.getDefaultBranch(),
                r.getVisibility(),
                r.getStatus(),
                r.getServiceId(),
                r.getWizardId(),
                r.getCreatedAt()
        );
    }
}
