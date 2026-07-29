package io.nimbus.platform.environment.dto;

import io.nimbus.platform.environment.domain.PromotionStatus;
import io.nimbus.platform.serviceapp.domain.EnvironmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ConfigDtos {

    private ConfigDtos() {
    }

    public record UpsertVariableRequest(
            @NotBlank
            @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "key must be UPPER_SNAKE_CASE")
            @Size(max = 128)
            String key,
            @NotBlank @Size(max = 500) String value
    ) {
    }

    public record UpdateVariableRequest(
            @NotBlank @Size(max = 500) String value
    ) {
    }

    public record VariableResponse(
            UUID id,
            UUID environmentId,
            String key,
            String value,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record UpsertSecretRequest(
            @NotBlank
            @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "key must be UPPER_SNAKE_CASE")
            @Size(max = 128)
            String key,
            @NotBlank @Size(max = 2000) String value
    ) {
    }

    public record UpdateSecretRequest(
            @NotBlank @Size(max = 2000) String value
    ) {
    }

    /**
     * value 가 있으면 해당 값으로, 없거나 generateRandom=true 이면 플랫폼이 생성.
     */
    public record RotateSecretRequest(
            @Size(max = 2000) String value,
            Boolean generateRandom
    ) {
    }

    public record RotateSecretResponse(
            UUID id,
            String key,
            String maskedValue,
            Integer version,
            boolean generated,
            /** 생성 시에만 1회 반환 (저장 후 재조회 불가) */
            String plainValueOnce,
            Instant rotatedAt
    ) {
    }

    /** 목록/조회: 값은 마스킹. reveal API 에서만 평문. */
    public record SecretResponse(
            UUID id,
            UUID environmentId,
            String key,
            String maskedValue,
            Integer version,
            Instant createdAt,
            Instant updatedAt,
            Instant lastSyncedAt,
            String lastSyncStatus,
            String lastSyncMessage
    ) {
    }

    public record SecretSyncRequest(
            /** null/empty = 전체 시크릿 */
            java.util.List<String> keys
    ) {
    }

    public record SecretSyncItem(
            String key,
            String status,
            String message
    ) {
    }

    public record SecretSyncResponse(
            String mode,
            String repository,
            int attempted,
            int succeeded,
            int failed,
            java.util.List<SecretSyncItem> items,
            String message
    ) {
    }

    public record SecretRevealResponse(
            UUID id,
            String key,
            String value,
            Integer version
    ) {
    }

    public record PromoteRequest(
            @NotNull EnvironmentType target
    ) {
    }

    public record PromoteResponse(
            UUID promotionId,
            PromotionStatus status,
            UUID sourceEnvironmentId,
            UUID targetEnvironmentId,
            EnvironmentType sourceType,
            EnvironmentType targetType,
            int variablesCopied,
            int secretsCopied,
            String message,
            Instant finishedAt,
            String gitOpsMode,
            String gitOpsHeadBranch,
            String gitOpsBaseBranch,
            String pullRequestUrl,
            Integer pullRequestNumber
    ) {
    }

    public record PromotionListResponse(
            List<PromoteResponse> items
    ) {
    }
}
