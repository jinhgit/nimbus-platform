package io.nimbus.platform.wizard.service;

import io.nimbus.platform.ai.dto.AiDtos;
import io.nimbus.platform.ai.service.RuleBasedAiService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.catalog.domain.ServiceTemplate;
import io.nimbus.platform.catalog.service.CatalogService;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.project.domain.Project;
import io.nimbus.platform.project.domain.ProjectStatus;
import io.nimbus.platform.project.repository.ProjectRepository;
import io.nimbus.platform.serviceapp.domain.AppService;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import io.nimbus.platform.serviceapp.repository.AppServiceRepository;
import io.nimbus.platform.wizard.domain.ServiceWizard;
import io.nimbus.platform.wizard.domain.WizardStatus;
import io.nimbus.platform.wizard.dto.WizardDtos;
import io.nimbus.platform.wizard.repository.ServiceWizardRepository;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WizardService {

    private final ServiceWizardRepository wizardRepository;
    private final ProjectRepository projectRepository;
    private final AppServiceRepository appServiceRepository;
    private final CatalogService catalogService;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final RuleBasedAiService aiService;
    private final WizardProvisionRunner provisionRunner;
    private final ObjectMapper objectMapper;

    public WizardService(
            ServiceWizardRepository wizardRepository,
            ProjectRepository projectRepository,
            AppServiceRepository appServiceRepository,
            CatalogService catalogService,
            WorkspaceBootstrapService workspaceBootstrapService,
            RuleBasedAiService aiService,
            WizardProvisionRunner provisionRunner,
            ObjectMapper objectMapper
    ) {
        this.wizardRepository = wizardRepository;
        this.projectRepository = projectRepository;
        this.appServiceRepository = appServiceRepository;
        this.catalogService = catalogService;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.aiService = aiService;
        this.provisionRunner = provisionRunner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WizardDtos.WizardResponse create(NimbusPrincipal principal, WizardDtos.CreateWizardRequest request) {
        Project project = requireProjectMember(principal, request.projectId());
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.PROJECT_ARCHIVED);
        }
        if (appServiceRepository.existsByProjectIdAndNameAndDeletedAtIsNull(project.getId(), request.serviceName())) {
            throw new BusinessException(ErrorCode.SERVICE_NAME_DUPLICATE);
        }

        RuntimeType runtime = null;
        UUID templateId = request.templateId();
        if (templateId != null) {
            ServiceTemplate template = catalogService.requirePublished(templateId);
            runtime = template.getRuntime();
        }

        ServiceWizard wizard = ServiceWizard.create(
                project.getId(),
                project.getWorkspaceId(),
                request.serviceName(),
                templateId,
                principal.userId()
        );
        if (runtime != null) {
            wizard.updateDraft(null, templateId, runtime, EnvironmentType.DEV,
                    null, null, null, null, null, null, null, 1);
        }
        return toResponse(wizardRepository.save(wizard));
    }

    @Transactional(readOnly = true)
    public WizardDtos.WizardResponse get(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = requireWizardAccess(principal, wizardId);
        return toResponse(wizard);
    }

    @Transactional(readOnly = true)
    public List<WizardDtos.WizardResponse> history(NimbusPrincipal principal, UUID projectId) {
        if (projectId != null) {
            requireProjectMember(principal, projectId);
            return wizardRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(projectId)
                    .stream().map(this::toResponse).toList();
        }
        return wizardRepository.findByCreatedByAndDeletedAtIsNullOrderByCreatedAtDesc(principal.userId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public WizardDtos.WizardResponse update(
            NimbusPrincipal principal,
            UUID wizardId,
            WizardDtos.UpdateWizardRequest request
    ) {
        ServiceWizard wizard = requireWizardAccess(principal, wizardId);
        if (wizard.getStatus() != WizardStatus.DRAFT) {
            throw new BusinessException(ErrorCode.WIZARD_INVALID_STATE, "DRAFT 상태에서만 수정 가능합니다");
        }
        if (request.templateId() != null) {
            catalogService.requirePublished(request.templateId());
        }
        wizard.updateDraft(
                request.serviceName(),
                request.templateId(),
                request.runtime(),
                request.environmentType(),
                request.databaseType(),
                request.cacheType(),
                request.replicaCount(),
                request.hpaEnabled(),
                request.cpu(),
                request.memory(),
                request.domain(),
                request.currentStep()
        );
        return toResponse(wizardRepository.save(wizard));
    }

    @Transactional
    public AiDtos.RecommendationResponse recommend(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = requireWizardAccess(principal, wizardId);
        if (wizard.getStatus() != WizardStatus.DRAFT) {
            throw new BusinessException(ErrorCode.WIZARD_INVALID_STATE);
        }
        AiDtos.RecommendationResponse rec = aiService.recommend(new AiDtos.RecommendationRequest(
                wizard.getServiceName(),
                null,
                wizard.getRuntime(),
                wizard.getEnvironmentType(),
                wizard.getEnvironmentType() == EnvironmentType.PRODUCTION ? "HIGH" : "MEDIUM"
        ));
        try {
            wizard.applyRecommendation(
                    rec.runtime(),
                    rec.database(),
                    rec.cache(),
                    rec.replicaCount(),
                    rec.hpaEnabled(),
                    objectMapper.writeValueAsString(rec)
            );
            if (rec.cpu() != null || rec.memory() != null) {
                wizard.updateDraft(null, null, null, null, null, null, null, null,
                        rec.cpu(), rec.memory(), null, 4);
            }
            wizardRepository.save(wizard);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RECOMMENDATION_FAILED, e.getMessage());
        }
        return rec;
    }

    @Transactional(readOnly = true)
    public WizardDtos.ValidateResponse validate(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = requireWizardAccess(principal, wizardId);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (wizard.getServiceName() == null || wizard.getServiceName().length() < 3) {
            errors.add("Service name is required (min 3)");
        }
        if (wizard.getTemplateId() == null) {
            errors.add("Template is required");
        } else {
            try {
                catalogService.requirePublished(wizard.getTemplateId());
            } catch (BusinessException ex) {
                errors.add("Template not published or missing");
            }
        }
        if (wizard.getRuntime() == null) {
            errors.add("Runtime is required");
        }
        if (wizard.getEnvironmentType() == null) {
            errors.add("Environment is required");
        }
        if (appServiceRepository.existsByProjectIdAndNameAndDeletedAtIsNull(
                wizard.getProjectId(), wizard.getServiceName())) {
            errors.add("Service name already exists in project");
        }
        if (wizard.getEnvironmentType() == EnvironmentType.PRODUCTION
                && (wizard.getReplicaCount() == null || wizard.getReplicaCount() < 2)) {
            warnings.add("Production with replica < 2 reduces availability");
        }
        if (wizard.getRecommendationJson() == null) {
            warnings.add("AI Recommendation not applied yet");
        }
        return new WizardDtos.ValidateResponse(errors.isEmpty(), warnings, errors);
    }

    @Transactional
    public WizardDtos.PreviewResponse preview(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = requireWizardAccess(principal, wizardId);
        WizardDtos.PreviewResponse preview = buildPreview(wizard);
        try {
            wizard.setPreviewJson(objectMapper.writeValueAsString(preview));
            wizard.updateDraft(null, null, null, null, null, null, null, null, null, null, null, 5);
            wizardRepository.save(wizard);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Preview serialize failed");
        }
        return preview;
    }

    @Transactional
    public WizardDtos.ExecuteResponse execute(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = requireWizardAccess(principal, wizardId);
        if (wizard.getStatus() != WizardStatus.DRAFT) {
            throw new BusinessException(ErrorCode.WIZARD_ALREADY_EXECUTED);
        }
        WizardDtos.ValidateResponse validation = validate(principal, wizardId);
        if (!validation.valid()) {
            throw new BusinessException(ErrorCode.WIZARD_VALIDATION_FAILED,
                    String.join(", ", validation.errors()));
        }
        // ensure preview exists
        if (wizard.getPreviewJson() == null) {
            preview(principal, wizardId);
            wizard = requireWizardAccess(principal, wizardId);
        }

        wizard.startProvisioning();
        wizardRepository.save(wizard);
        provisionRunner.runAsync(wizard.getId());
        return new WizardDtos.ExecuteResponse(
                wizard.getId(),
                wizard.getId(),
                wizard.getStatus(),
                wizard.getProgress()
        );
    }

    @Transactional
    public WizardDtos.WizardResponse cancel(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = requireWizardAccess(principal, wizardId);
        if (wizard.getStatus() != WizardStatus.DRAFT
                && wizard.getStatus() != WizardStatus.PROVISIONING
                && wizard.getStatus() != WizardStatus.VALIDATING) {
            throw new BusinessException(ErrorCode.WIZARD_CANCEL_DENIED);
        }
        if (wizard.getStatus() == WizardStatus.DEPLOYING) {
            throw new BusinessException(ErrorCode.WIZARD_CANCEL_DENIED, "Deploy 중에는 취소할 수 없습니다");
        }
        wizard.cancel();
        return toResponse(wizardRepository.save(wizard));
    }

    @Transactional(readOnly = true)
    public WizardDtos.WizardLogsResponse logs(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = requireWizardAccess(principal, wizardId);
        return new WizardDtos.WizardLogsResponse(
                wizard.getId(),
                wizard.getStatus(),
                wizard.getProgress(),
                wizard.getProgressMessage(),
                wizard.getLogs()
        );
    }

    WizardDtos.PreviewResponse buildPreview(ServiceWizard wizard) {
        ServiceTemplate template = wizard.getTemplateId() != null
                ? catalogService.require(wizard.getTemplateId())
                : null;
        String runtime = wizard.getRuntime() != null
                ? wizard.getRuntime().name()
                : (template != null ? template.getRuntime().name() : "UNKNOWN");
        String name = wizard.getServiceName();
        int replicas = wizard.getReplicaCount() != null ? wizard.getReplicaCount() : 1;
        boolean hpa = Boolean.TRUE.equals(wizard.getHpaEnabled());
        String cpu = wizard.getCpu() != null ? wizard.getCpu() : "500m";
        String memory = wizard.getMemory() != null ? wizard.getMemory() : "512Mi";
        String db = wizard.getDatabaseType() != null ? wizard.getDatabaseType() : "NONE";
        String cache = wizard.getCacheType() != null ? wizard.getCacheType() : "NONE";
        String env = wizard.getEnvironmentType() != null ? wizard.getEnvironmentType().name() : "DEV";

        Map<String, String> structure = new LinkedHashMap<>();
        structure.put("README.md", "Service overview");
        structure.put("src/", "Application source");
        structure.put("helm/values.yaml", "Helm values");
        structure.put("terraform/main.tf", "Infra variables (local)");
        structure.put(".github/workflows/ci-cd.yml", "GitHub Actions");
        structure.put("Dockerfile", "Container build");
        structure.put("argocd/application.yaml", "ArgoCD Application");

        String blueprint = """
                name: %s
                runtime: %s
                environment: %s
                database: %s
                cache: %s
                replica: %d
                hpa: %s
                resources:
                  cpu: %s
                  memory: %s
                """.formatted(name, runtime, env, db, cache, replicas, hpa, cpu, memory);

        String helm = template != null && template.getDefaultHelmValues() != null
                ? template.getDefaultHelmValues()
                : "";
        helm = """
                # generated for %s
                replicaCount: %d
                image:
                  repository: %s
                  tag: "1.0.0"
                resources:
                  requests:
                    cpu: %s
                    memory: %s
                autoscaling:
                  enabled: %s
                  minReplicas: %d
                  maxReplicas: %d
                env:
                  DATABASE: %s
                  CACHE: %s
                """.formatted(name, replicas, name, cpu, memory, hpa, Math.max(1, replicas), Math.max(replicas * 3, 5), db, cache)
                + "\n# template defaults\n" + helm;

        String tf = """
                # terraform.tfvars (local / free-only — apply optional)
                project     = "%s"
                environment = "%s"
                replica     = %d
                cpu         = "%s"
                memory      = "%s"
                """.formatted(name, env.toLowerCase(), replicas, cpu, memory);

        String actions = template != null && template.getDefaultWorkflow() != null
                ? template.getDefaultWorkflow()
                : """
                name: ci-cd
                on:
                  push:
                    branches: [ main ]
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v4
                      - run: echo "build %s"
                """.formatted(name);

        String deploymentYaml = """
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: %s
                  labels:
                    app: %s
                spec:
                  replicas: %d
                  selector:
                    matchLabels:
                      app: %s
                  template:
                    metadata:
                      labels:
                        app: %s
                    spec:
                      containers:
                        - name: app
                          image: %s:1.0.0
                          ports:
                            - containerPort: 8080
                          resources:
                            requests:
                              cpu: %s
                              memory: %s
                          readinessProbe:
                            httpGet:
                              path: /actuator/health
                              port: 8080
                """.formatted(name, name, replicas, name, name, name, cpu, memory);

        String argo = """
                apiVersion: argoproj.io/v1alpha1
                kind: Application
                metadata:
                  name: %s
                  namespace: argocd
                spec:
                  project: default
                  source:
                    repoURL: https://github.com/nimbus-demo/%s.git
                    path: helm
                    targetRevision: HEAD
                  destination:
                    server: https://kubernetes.default.svc
                    namespace: %s
                  syncPolicy:
                    automated:
                      prune: true
                      selfHeal: true
                """.formatted(name, name, name + "-" + env.toLowerCase());

        return new WizardDtos.PreviewResponse(
                name,
                runtime,
                env,
                structure,
                blueprint,
                helm,
                tf,
                actions,
                deploymentYaml,
                argo
        );
    }

    private Project requireProjectMember(NimbusPrincipal principal, UUID projectId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        workspaceBootstrapService.requireMember(project.getWorkspaceId(), principal.userId());
        return project;
    }

    private ServiceWizard requireWizardAccess(NimbusPrincipal principal, UUID wizardId) {
        ServiceWizard wizard = wizardRepository.findByIdAndDeletedAtIsNull(wizardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WIZARD_NOT_FOUND));
        workspaceBootstrapService.requireMember(wizard.getWorkspaceId(), principal.userId());
        return wizard;
    }

    private WizardDtos.WizardResponse toResponse(ServiceWizard w) {
        Object recommendation = null;
        Object preview = null;
        try {
            if (w.getRecommendationJson() != null) {
                recommendation = objectMapper.readValue(w.getRecommendationJson(), Object.class);
            }
            if (w.getPreviewJson() != null) {
                preview = objectMapper.readValue(w.getPreviewJson(), Object.class);
            }
        } catch (Exception ignored) {
            recommendation = w.getRecommendationJson();
            preview = w.getPreviewJson();
        }
        return new WizardDtos.WizardResponse(
                w.getId(),
                w.getProjectId(),
                w.getWorkspaceId(),
                w.getServiceName(),
                w.getTemplateId(),
                w.getRuntime(),
                w.getEnvironmentType(),
                w.getDatabaseType(),
                w.getCacheType(),
                w.getReplicaCount(),
                w.getHpaEnabled(),
                w.getCpu(),
                w.getMemory(),
                w.getDomain(),
                w.getStatus(),
                w.getCurrentStep(),
                w.getProgress(),
                w.getProgressMessage(),
                w.getServiceId(),
                recommendation,
                preview,
                w.getCreatedAt(),
                w.getUpdatedAt()
        );
    }
}
