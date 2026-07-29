package io.nimbus.platform.incident.service;

import io.nimbus.platform.incident.domain.IncidentSeverity;
import io.nimbus.platform.incident.domain.IncidentSource;
import org.springframework.stereotype.Service;

/**
 * free-only rule-engine analysis for incidents.
 */
@Service
public class IncidentAnalysisService {

    public Analysis analyze(
            IncidentSource source,
            String title,
            String rawReason,
            String serviceName
    ) {
        String reason = rawReason != null ? rawReason : "";
        String lower = reason.toLowerCase();
        IncidentSeverity severity = IncidentSeverity.MEDIUM;
        StringBuilder analysis = new StringBuilder();
        analysis.append("## 규칙 기반 분석\n");
        analysis.append("- 서비스: ").append(serviceName != null ? serviceName : "—").append("\n");
        analysis.append("- 출처: ").append(source).append("\n");
        analysis.append("- 증상: ").append(title).append("\n\n");

        if (source == IncidentSource.SAGA) {
            severity = IncidentSeverity.HIGH;
            analysis.append("### 추정 원인\n");
            if (lower.contains("github") || lower.contains("scm") || lower.contains("token")) {
                analysis.append("- SCM/GitHub 연결 또는 토큰 권한 문제 가능\n");
            } else if (lower.contains("k8s") || lower.contains("kubernetes") || lower.contains("namespace")) {
                analysis.append("- Kubernetes 클러스터 연결/네임스페이스 권한 문제 가능\n");
            } else if (lower.contains("timeout")) {
                analysis.append("- 단계 타임아웃 — 네트워크 또는 의존 서비스 지연 가능\n");
            } else {
                analysis.append("- Provision Saga 단계 실패 — Wizard Saga 상세와 보상 로그 확인\n");
            }
            analysis.append("\n### 권장 조치\n");
            analysis.append("1. Service Wizard → Saga 단계 상태 확인\n");
            analysis.append("2. 실패 단계 메시지·보상 로그 검토\n");
            analysis.append("3. mutator 역할로 Retry (서비스 미생성 시)\n");
        } else if (source == IncidentSource.PIPELINE) {
            severity = IncidentSeverity.MEDIUM;
            analysis.append("### 추정 원인\n");
            analysis.append("- 빌드/이미지 파이프라인 실패 또는 Actions 워크플로 오류\n");
            analysis.append("\n### 권장 조치\n");
            analysis.append("1. Pipelines 로그 또는 GitHub Actions run 확인\n");
            analysis.append("2. Dockerfile / workflow 문법 점검\n");
            analysis.append("3. 필요 시 파이프라인 재실행\n");
        } else if (source == IncidentSource.ENVIRONMENT) {
            severity = IncidentSeverity.HIGH;
            analysis.append("### 추정 원인\n");
            analysis.append("- 환경 헬스 UNHEALTHY 또는 FAILED 상태\n");
            if (lower.contains("archived")) {
                analysis.append("- 환경이 보관(ARCHIVED) 상태일 수 있음\n");
            }
            analysis.append("\n### 권장 조치\n");
            analysis.append("1. Service Detail → Environment 헬스 재검사\n");
            analysis.append("2. 변수/시크릿·레플리카 설정 확인\n");
            analysis.append("3. 필요 시 이전 환경에서 재승격\n");
        } else {
            analysis.append("### 권장 조치\n- 관련 리소스와 감사 로그를 확인하세요.\n");
        }

        analysis.append("\n_provider: rule-engine_");
        String summary = title + (reason.isBlank() ? "" : " — " + truncate(reason, 120));
        return new Analysis(severity, summary, analysis.toString());
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public record Analysis(IncidentSeverity severity, String summary, String analysisText) {
    }
}
