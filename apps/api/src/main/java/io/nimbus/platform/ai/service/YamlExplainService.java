package io.nimbus.platform.ai.service;

import io.nimbus.platform.ai.dto.AiDtos;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML Explain — rule-engine (free-only).
 * 플랫폼이 생성한 YAML을 필드 단위로 해석한다.
 */
@Service
public class YamlExplainService {

    private static final Pattern REPLICAS = Pattern.compile("(?m)^\\s*replicas:\\s*(\\d+)");
    private static final Pattern IMAGE = Pattern.compile("(?m)^\\s*image:\\s*[\"']?([^\\s\"']+)");
    private static final Pattern KIND = Pattern.compile("(?m)^kind:\\s*(\\S+)");
    private static final Pattern MEMORY = Pattern.compile("(?m)^\\s*memory:\\s*[\"']?(\\S+)");
    private static final Pattern CPU = Pattern.compile("(?m)^\\s*cpu:\\s*[\"']?(\\S+)");

    public AiDtos.YamlExplainResponse explain(AiDtos.YamlExplainRequest request) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "YAML content is required");
        }
        String raw = request.content();
        String content = raw.toLowerCase(Locale.ROOT);
        EnvironmentType env = request.environmentType() != null
                ? request.environmentType()
                : EnvironmentType.DEV;
        boolean prod = env == EnvironmentType.PRODUCTION;

        AiDtos.YamlKind kind = request.kind() != null && request.kind() != AiDtos.YamlKind.AUTO
                ? request.kind()
                : detectKind(content, raw);

        List<AiDtos.YamlHighlight> highlights = new ArrayList<>();
        List<String> risks = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        switch (kind) {
            case HELM -> explainHelm(content, raw, prod, highlights, risks, suggestions);
            case ARGO -> explainArgo(content, prod, highlights, risks, suggestions);
            case ACTIONS -> explainActions(content, highlights, risks, suggestions);
            case TERRAFORM -> explainTerraform(content, highlights, risks, suggestions);
            case SERVICE -> explainService(content, highlights, risks, suggestions);
            case INGRESS -> explainIngress(content, highlights, risks, suggestions);
            case DEPLOYMENT, GENERIC, AUTO ->
                    explainDeploymentLike(content, raw, prod, highlights, risks, suggestions);
        }

        if (highlights.isEmpty()) {
            highlights.add(new AiDtos.YamlHighlight(
                    "document",
                    "INFO",
                    "문서 개요",
                    "표준 키워드가 많지 않은 매니페스트입니다. Deployment/Helm/Argo 형식이면 더 구체적인 설명이 가능합니다."
            ));
            suggestions.add("Wizard Preview의 Deployment / Helm / Argo 탭 YAML을 넣어 보세요.");
        }

        String summary = buildSummary(kind, request.serviceName(), env, risks.size());

        return new AiDtos.YamlExplainResponse(
                summary,
                kind,
                highlights,
                risks,
                suggestions,
                "rule-engine"
        );
    }

    private void explainDeploymentLike(
            String content,
            String raw,
            boolean prod,
            List<AiDtos.YamlHighlight> highlights,
            List<String> risks,
            List<String> suggestions
    ) {
        Matcher kindM = KIND.matcher(raw);
        if (kindM.find()) {
            highlights.add(new AiDtos.YamlHighlight(
                    "kind",
                    "INFO",
                    "Kind: " + kindM.group(1),
                    "Kubernetes 리소스 종류입니다. Deployment면 Pod 템플릿을 선언적으로 관리합니다."
            ));
        }

        Matcher rep = REPLICAS.matcher(raw);
        if (rep.find()) {
            int n = Integer.parseInt(rep.group(1));
            String level = (prod && n < 2) ? "WARN" : "INFO";
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.replicas",
                    level,
                    "Replicas = " + n,
                    n <= 1
                            ? "Pod를 1개만 유지합니다. 노드/Pod 장애 시 서비스가 중단될 수 있습니다."
                            : "동일 Pod를 " + n + "개 유지해 가용성과 부하 분산을 높입니다."
            ));
            if (prod && n < 2) {
                risks.add("Production에서 replicas < 2 → HA 불가");
                suggestions.add("replicas를 2 이상으로 올리거나 HPA를 검토하세요.");
            }
        }

        Matcher img = IMAGE.matcher(raw);
        if (img.find()) {
            String image = img.group(1);
            boolean latest = image.endsWith(":latest") || !image.contains(":");
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.template.spec.containers[].image",
                    latest ? "WARN" : "INFO",
                    "Image: " + image,
                    latest
                            ? "latest 또는 태그 없음은 배포 재현성이 떨어집니다. 고정 버전 태그를 권장합니다."
                            : "컨테이너 이미지와 태그입니다. 롤백·재현을 위해 시맨틱 버전 태그를 유지하세요."
            ));
            if (latest) {
                risks.add("image 태그가 latest이거나 불명확함");
                suggestions.add("예: nginx:1.27-alpine 처럼 고정 태그 사용");
            }
        }

        boolean hasRequests = content.contains("requests:");
        boolean hasLimits = content.contains("limits:");
        if (hasRequests || hasLimits) {
            Matcher mem = MEMORY.matcher(raw);
            Matcher cpu = CPU.matcher(raw);
            String memHint = mem.find() ? mem.group(1) : null;
            String cpuHint = cpu.find() ? cpu.group(1) : null;
            highlights.add(new AiDtos.YamlHighlight(
                    "resources",
                    "INFO",
                    "Resources",
                    "CPU/Memory " + (hasRequests ? "requests" : "")
                            + (hasRequests && hasLimits ? "/" : "")
                            + (hasLimits ? "limits" : "")
                            + " 설정."
                            + (memHint != null ? " memory≈" + memHint + "." : "")
                            + (cpuHint != null ? " cpu≈" + cpuHint + "." : "")
                            + " requests는 스케줄링 보장, limits는 상한입니다."
            ));
            if (hasRequests && !hasLimits) {
                suggestions.add("limits를 함께 두면 이웃 Pod noisy-neighbor 영향을 줄일 수 있습니다.");
            }
        } else if (content.contains("containers:") || content.contains("kind: deployment")) {
            risks.add("resources 블록이 보이지 않음");
            suggestions.add("requests/limits를 지정해 스케줄링·OOM 예측 가능성을 높이세요.");
            highlights.add(new AiDtos.YamlHighlight(
                    "resources",
                    "WARN",
                    "Resources 미지정",
                    "컨테이너 리소스 요청/제한이 없으면 노드 과밀·OOM 진단이 어려워집니다."
            ));
        }

        boolean readiness = content.contains("readinessprobe") || content.contains("readiness_probe");
        boolean liveness = content.contains("livenessprobe") || content.contains("liveness_probe");
        if (readiness) {
            highlights.add(new AiDtos.YamlHighlight(
                    "readinessProbe",
                    "INFO",
                    "Readiness Probe",
                    "Ready 상태가 될 때까지 Service 엔드포인트에 포함하지 않습니다. 기동 중 502를 줄입니다."
            ));
        } else if (content.contains("kind: deployment") || content.contains("containers:")) {
            risks.add("readinessProbe 없음");
            suggestions.add("HTTP/TCP readinessProbe를 추가하세요.");
            highlights.add(new AiDtos.YamlHighlight(
                    "readinessProbe",
                    "WARN",
                    "Readiness Probe 없음",
                    "기동 중인 Pod로 트래픽이 유입될 수 있습니다."
            ));
        }
        if (liveness) {
            highlights.add(new AiDtos.YamlHighlight(
                    "livenessProbe",
                    "INFO",
                    "Liveness Probe",
                    "컨테이너가 응답 불능이면 kubelet이 재시작합니다. 과도하게 빡센 설정은 재시작 루프를 유발할 수 있습니다."
            ));
        }

        if (content.contains("env:") || content.contains("envfrom:")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "env",
                    "INFO",
                    "Environment variables",
                    "컨테이너 환경 변수입니다. 시크릿은 plain env 대신 Secret/EnvFrom 참조를 권장합니다."
            ));
            if (content.contains("password") || content.contains("secret") || content.contains("apikey")) {
                risks.add("YAML에 시크릿성 키워드가 평문으로 보일 수 있음");
                suggestions.add("민감 값은 Secret 리소스 또는 외부 시크릿 동기화를 사용하세요.");
            }
        }

        if (content.contains("ports:") || content.contains("containerport")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "ports",
                    "INFO",
                    "Ports",
                    "컨테이너/서비스가 노출하는 포트입니다. Service targetPort와 일치해야 트래픽이 전달됩니다."
            ));
        }

        if (content.contains("hpa") || content.contains("horizontalpodautoscaler") || content.contains("autoscaling")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "hpa",
                    "INFO",
                    "Autoscaling",
                    "부하에 따라 레플리카를 자동 조절합니다. Production에서 권장됩니다."
            ));
        } else if (prod) {
            suggestions.add("Production이면 HPA(HorizontalPodAutoscaler) 검토를 권장합니다.");
        }

        if (content.contains("securitycontext") || content.contains("runasnonroot")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "securityContext",
                    "INFO",
                    "Security Context",
                    "권한 축소(non-root, readOnlyRootFilesystem 등)로 공격 면을 줄입니다."
            ));
        }
    }

    private void explainHelm(
            String content,
            String raw,
            boolean prod,
            List<AiDtos.YamlHighlight> highlights,
            List<String> risks,
            List<String> suggestions
    ) {
        highlights.add(new AiDtos.YamlHighlight(
                "helm",
                "INFO",
                "Helm values",
                "차트에 주입되는 설정 값입니다. 이미지·레플리카·리소스·인그레스 등을 환경별로 분기합니다."
        ));
        explainDeploymentLike(content, raw, prod, highlights, risks, suggestions);
        if (content.contains("image:") || content.contains("repository:")) {
            suggestions.add("환경별 values-dev.yaml / values-prod.yaml 분리를 권장합니다.");
        }
        if (content.contains("ingress:") && content.contains("enabled: true")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "ingress",
                    "INFO",
                    "Ingress enabled",
                    "외부 HTTP(S) 진입점입니다. TLS 인증서·호스트 규칙을 함께 확인하세요."
            ));
        }
    }

    private void explainArgo(
            String content,
            boolean prod,
            List<AiDtos.YamlHighlight> highlights,
            List<String> risks,
            List<String> suggestions
    ) {
        highlights.add(new AiDtos.YamlHighlight(
                "kind",
                "INFO",
                "Argo CD Application",
                "Git 저장소의 매니페스트를 클러스터에 동기화하는 GitOps 리소스입니다."
        ));
        if (content.contains("automated") || content.contains("selfheal") || content.contains("prune")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.syncPolicy",
                    "INFO",
                    "Automated sync",
                    "Git 변경 시 자동 동기화·selfHeal·prune 정책입니다. Production에서는 수동 승인 정책을 검토하세요."
            ));
            if (prod && content.contains("automated")) {
                suggestions.add("Production Automated Sync는 승인 게이트와 함께 쓰는 것을 권장합니다.");
            }
        } else {
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.syncPolicy",
                    "INFO",
                    "수동 Sync 가능성",
                    "자동 sync 블록이 보이지 않으면 운영자가 수동으로 Sync할 수 있습니다."
            ));
        }
        if (content.contains("sourcerepo") || content.contains("repoURL") || content.contains("repourl")
                || content.contains("path:")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.source",
                    "INFO",
                    "Git source",
                    "동기화할 Git repo/path/targetRevision입니다. 브랜치·디렉터리가 배포 단위와 일치해야 합니다."
            ));
        }
        if (content.contains("destination") || content.contains("namespace:")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.destination",
                    "INFO",
                    "Destination",
                    "적용 대상 클러스터와 네임스페이스입니다."
            ));
        }
    }

    private void explainActions(
            String content,
            List<AiDtos.YamlHighlight> highlights,
            List<String> risks,
            List<String> suggestions
    ) {
        highlights.add(new AiDtos.YamlHighlight(
                "workflow",
                "INFO",
                "GitHub Actions",
                "CI/CD 워크플로입니다. push/PR 트리거로 빌드·테스트·배포 단계를 실행합니다."
        ));
        if (content.contains("on:") || content.contains("push:") || content.contains("pull_request")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "on",
                    "INFO",
                    "Triggers",
                    "워크플로가 언제 실행되는지 정의합니다."
            ));
        }
        if (content.contains("docker") || content.contains("build-push") || content.contains("buildx")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "jobs.build",
                    "INFO",
                    "Image build",
                    "컨테이너 이미지 빌드/푸시 단계로 보입니다. 레지스트리 시크릿이 필요합니다."
            ));
        }
        if (content.contains("secrets.") || content.contains("${{ secrets")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "secrets",
                    "INFO",
                    "GitHub Secrets",
                    "저장소/환경 시크릿을 주입합니다. Nimbus Secret Sync와 맞출 수 있습니다."
            ));
        } else {
            suggestions.add("배포 단계가 있다면 GitHub Secrets로 자격 증명을 주입하세요.");
        }
    }

    private void explainTerraform(
            String content,
            List<AiDtos.YamlHighlight> highlights,
            List<String> risks,
            List<String> suggestions
    ) {
        highlights.add(new AiDtos.YamlHighlight(
                "terraform",
                "INFO",
                "Terraform / tfvars",
                "인프라 코드 입력 변수입니다. 클러스터·네트워크·노드 크기 등을 선언합니다."
        ));
        if (content.contains("region") || content.contains("vpc") || content.contains("cluster")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "variables",
                    "INFO",
                    "Infra variables",
                    "리전·VPC·클러스터 관련 변수로 보입니다. free-only 경로에서는 로컬 클러스터 대체 값을 쓸 수 있습니다."
            ));
        }
        suggestions.add("실 apply 전 plan 결과를 검토하고, 상태 파일 백엔드를 안전하게 관리하세요.");
    }

    private void explainService(
            String content,
            List<AiDtos.YamlHighlight> highlights,
            List<String> risks,
            List<String> suggestions
    ) {
        highlights.add(new AiDtos.YamlHighlight(
                "kind",
                "INFO",
                "Service",
                "Pod 집합에 안정적인 네트워크 엔드포인트를 제공합니다."
        ));
        if (content.contains("type: loadbalancer") || content.contains("type: \"loadbalancer\"")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.type",
                    "INFO",
                    "LoadBalancer",
                    "클라우드 LB를 할당합니다. 로컬 kind/k3d에서는 NodePort/ClusterIP가 흔합니다."
            ));
        } else if (content.contains("clusterip") || content.contains("type: clusterip")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.type",
                    "INFO",
                    "ClusterIP",
                    "클러스터 내부에서만 접근 가능한 기본 Service 타입입니다."
            ));
        }
        if (content.contains("selector:")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.selector",
                    "INFO",
                    "Selector",
                    "라벨이 일치하는 Pod로 트래픽을 라우팅합니다. Deployment 템플릿 라벨과 일치해야 합니다."
            ));
        }
    }

    private void explainIngress(
            String content,
            List<AiDtos.YamlHighlight> highlights,
            List<String> risks,
            List<String> suggestions
    ) {
        highlights.add(new AiDtos.YamlHighlight(
                "kind",
                "INFO",
                "Ingress",
                "HTTP(S) L7 라우팅 규칙입니다. 호스트·경로를 Service로 보냅니다."
        ));
        if (content.contains("tls:") || content.contains("secretname")) {
            highlights.add(new AiDtos.YamlHighlight(
                    "spec.tls",
                    "INFO",
                    "TLS",
                    "HTTPS 종료용 인증서 Secret 참조입니다."
            ));
        } else {
            suggestions.add("외부 노출 시 TLS를 구성하세요.");
        }
    }

    private AiDtos.YamlKind detectKind(String contentLower, String raw) {
        if (contentLower.contains("kind: application") && (contentLower.contains("argoproj") || contentLower.contains("argocd"))) {
            return AiDtos.YamlKind.ARGO;
        }
        if (contentLower.contains("kind: application") && contentLower.contains("syncpolicy")) {
            return AiDtos.YamlKind.ARGO;
        }
        if (raw.contains("apiVersion: argoproj.io") || contentLower.contains("argoproj.io/v1alpha1")) {
            return AiDtos.YamlKind.ARGO;
        }
        if (contentLower.contains("kind: deployment")) {
            return AiDtos.YamlKind.DEPLOYMENT;
        }
        if (contentLower.contains("kind: service") && !contentLower.contains("kind: deployment")) {
            return AiDtos.YamlKind.SERVICE;
        }
        if (contentLower.contains("kind: ingress")) {
            return AiDtos.YamlKind.INGRESS;
        }
        if (contentLower.contains("jobs:") && (contentLower.contains("runs-on:") || contentLower.contains("uses:"))) {
            return AiDtos.YamlKind.ACTIONS;
        }
        if (contentLower.contains("replicaCount".toLowerCase()) || contentLower.contains("image:")
                && contentLower.contains("repository:") && !contentLower.contains("apiversion:")) {
            return AiDtos.YamlKind.HELM;
        }
        if (contentLower.contains("resource \"") || contentLower.contains("variable \"")
                || contentLower.contains("terraform") || contentLower.matches("(?s).*\\w+\\s*=\\s*\".*")) {
            // loose tfvars detection
            if (contentLower.contains("=") && !contentLower.contains("apiversion:")) {
                return AiDtos.YamlKind.TERRAFORM;
            }
        }
        if (contentLower.contains("values:") || (contentLower.contains("image:") && contentLower.contains("tag:"))) {
            return AiDtos.YamlKind.HELM;
        }
        return AiDtos.YamlKind.GENERIC;
    }

    private String buildSummary(
            AiDtos.YamlKind kind,
            String serviceName,
            EnvironmentType env,
            int riskCount
    ) {
        String svc = serviceName != null && !serviceName.isBlank() ? serviceName : "service";
        String base = switch (kind) {
            case DEPLOYMENT -> svc + " 의 Kubernetes Deployment 매니페스트 설명입니다.";
            case HELM -> svc + " 의 Helm values 해석입니다.";
            case ARGO -> svc + " 의 Argo CD Application (GitOps) 설명입니다.";
            case ACTIONS -> "GitHub Actions 워크플로 설명입니다.";
            case TERRAFORM -> "Terraform/변수 파일 해석입니다.";
            case SERVICE -> "Kubernetes Service 리소스 설명입니다.";
            case INGRESS -> "Ingress 라우팅 설명입니다.";
            default -> "YAML/매니페스트 rule-engine 설명입니다.";
        };
        String envPart = env != null ? " 환경: " + env.name() + "." : "";
        String riskPart = riskCount > 0
                ? " 주의 포인트 " + riskCount + "건."
                : " 치명적 패턴은 크게 보이지 않습니다.";
        return base + envPart + riskPart;
    }
}
