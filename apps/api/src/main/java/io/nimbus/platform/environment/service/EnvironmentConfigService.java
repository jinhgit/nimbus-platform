package io.nimbus.platform.environment.service;

import io.nimbus.platform.audit.domain.AuditAction;
import io.nimbus.platform.audit.service.AuditService;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.common.exception.BusinessException;
import io.nimbus.platform.common.exception.ErrorCode;
import io.nimbus.platform.environment.domain.EnvSecret;
import io.nimbus.platform.environment.domain.EnvVariable;
import io.nimbus.platform.environment.domain.EnvironmentStatus;
import io.nimbus.platform.environment.domain.ServiceEnvironment;
import io.nimbus.platform.environment.dto.ConfigDtos;
import io.nimbus.platform.environment.repository.EnvSecretRepository;
import io.nimbus.platform.environment.repository.EnvVariableRepository;
import io.nimbus.platform.environment.repository.ServiceEnvironmentRepository;
import io.nimbus.platform.github.crypto.TokenCryptoService;
import io.nimbus.platform.workspace.service.WorkspaceBootstrapService;
import io.nimbus.platform.workspace.service.WorkspacePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EnvironmentConfigService {

    private final ServiceEnvironmentRepository environmentRepository;
    private final EnvVariableRepository variableRepository;
    private final EnvSecretRepository secretRepository;
    private final WorkspaceBootstrapService workspaceBootstrapService;
    private final WorkspacePermissionService workspacePermissionService;
    private final TokenCryptoService tokenCryptoService;
    private final AuditService auditService;

    public EnvironmentConfigService(
            ServiceEnvironmentRepository environmentRepository,
            EnvVariableRepository variableRepository,
            EnvSecretRepository secretRepository,
            WorkspaceBootstrapService workspaceBootstrapService,
            WorkspacePermissionService workspacePermissionService,
            TokenCryptoService tokenCryptoService,
            AuditService auditService
    ) {
        this.environmentRepository = environmentRepository;
        this.variableRepository = variableRepository;
        this.secretRepository = secretRepository;
        this.workspaceBootstrapService = workspaceBootstrapService;
        this.workspacePermissionService = workspacePermissionService;
        this.tokenCryptoService = tokenCryptoService;
        this.auditService = auditService;
    }

    // ── Variables ──────────────────────────────────────────

    @Transactional
    public ConfigDtos.VariableResponse createVariable(
            NimbusPrincipal principal,
            UUID environmentId,
            ConfigDtos.UpsertVariableRequest request
    ) {
        ServiceEnvironment env = requireWritableEnv(principal, environmentId);
        String key = request.key().trim().toUpperCase();
        if (variableRepository.existsByEnvironmentIdAndKeyAndDeletedAtIsNull(environmentId, key)) {
            throw new BusinessException(ErrorCode.CONFIG_KEY_DUPLICATE, "Variable already exists: " + key);
        }
        EnvVariable saved = variableRepository.save(EnvVariable.create(
                env.getId(), env.getServiceId(), env.getWorkspaceId(),
                key, request.value(), principal.userId()
        ));
        auditService.recordSuccess(
                principal, AuditAction.CREATE_VARIABLE, "VARIABLE",
                saved.getId(), key, env.getWorkspaceId(),
                "Variable set on " + env.getType()
        );
        return toVariable(saved);
    }

    @Transactional(readOnly = true)
    public List<ConfigDtos.VariableResponse> listVariables(NimbusPrincipal principal, UUID environmentId) {
        requireEnvMember(principal, environmentId);
        return variableRepository.findByEnvironmentIdAndDeletedAtIsNullOrderByKeyAsc(environmentId)
                .stream().map(this::toVariable).toList();
    }

    @Transactional
    public ConfigDtos.VariableResponse updateVariable(
            NimbusPrincipal principal,
            UUID variableId,
            ConfigDtos.UpdateVariableRequest request
    ) {
        EnvVariable variable = variableRepository.findByIdAndDeletedAtIsNull(variableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIABLE_NOT_FOUND));
        requireWritableEnv(principal, variable.getEnvironmentId());
        variable.updateValue(request.value());
        EnvVariable saved = variableRepository.save(variable);
        auditService.recordSuccess(
                principal, AuditAction.UPDATE_VARIABLE, "VARIABLE",
                saved.getId(), saved.getKey(), saved.getWorkspaceId(),
                "Variable updated"
        );
        return toVariable(saved);
    }

    @Transactional
    public void deleteVariable(NimbusPrincipal principal, UUID variableId) {
        EnvVariable variable = variableRepository.findByIdAndDeletedAtIsNull(variableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VARIABLE_NOT_FOUND));
        requireWritableEnv(principal, variable.getEnvironmentId());
        variable.softDelete();
        variableRepository.save(variable);
        auditService.recordSuccess(
                principal, AuditAction.DELETE_VARIABLE, "VARIABLE",
                variable.getId(), variable.getKey(), variable.getWorkspaceId(),
                "Variable deleted"
        );
    }

    // ── Secrets ────────────────────────────────────────────

    @Transactional
    public ConfigDtos.SecretResponse createSecret(
            NimbusPrincipal principal,
            UUID environmentId,
            ConfigDtos.UpsertSecretRequest request
    ) {
        ServiceEnvironment env = requireWritableEnv(principal, environmentId);
        String key = request.key().trim().toUpperCase();
        if (secretRepository.existsByEnvironmentIdAndKeyAndDeletedAtIsNull(environmentId, key)) {
            throw new BusinessException(ErrorCode.CONFIG_KEY_DUPLICATE, "Secret already exists: " + key);
        }
        String enc = tokenCryptoService.encrypt(request.value());
        EnvSecret saved = secretRepository.save(EnvSecret.create(
                env.getId(), env.getServiceId(), env.getWorkspaceId(),
                key, enc, principal.userId()
        ));
        auditService.recordSuccess(
                principal, AuditAction.CREATE_SECRET, "SECRET",
                saved.getId(), key, env.getWorkspaceId(),
                "Secret set on " + env.getType()
        );
        return toSecretMasked(saved);
    }

    @Transactional(readOnly = true)
    public List<ConfigDtos.SecretResponse> listSecrets(NimbusPrincipal principal, UUID environmentId) {
        requireEnvMember(principal, environmentId);
        return secretRepository.findByEnvironmentIdAndDeletedAtIsNullOrderByKeyAsc(environmentId)
                .stream().map(this::toSecretMasked).toList();
    }

    @Transactional
    public ConfigDtos.SecretResponse updateSecret(
            NimbusPrincipal principal,
            UUID secretId,
            ConfigDtos.UpdateSecretRequest request
    ) {
        EnvSecret secret = secretRepository.findByIdAndDeletedAtIsNull(secretId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECRET_NOT_FOUND));
        requireWritableEnv(principal, secret.getEnvironmentId());
        secret.rotate(tokenCryptoService.encrypt(request.value()));
        EnvSecret saved = secretRepository.save(secret);
        auditService.recordSuccess(
                principal, AuditAction.UPDATE_SECRET, "SECRET",
                saved.getId(), saved.getKey(), saved.getWorkspaceId(),
                "Secret rotated v" + saved.getRotationVersion()
        );
        return toSecretMasked(saved);
    }

    @Transactional
    public void deleteSecret(NimbusPrincipal principal, UUID secretId) {
        EnvSecret secret = secretRepository.findByIdAndDeletedAtIsNull(secretId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECRET_NOT_FOUND));
        requireWritableEnv(principal, secret.getEnvironmentId());
        secret.softDelete();
        secretRepository.save(secret);
        auditService.recordSuccess(
                principal, AuditAction.DELETE_SECRET, "SECRET",
                secret.getId(), secret.getKey(), secret.getWorkspaceId(),
                "Secret deleted"
        );
    }

    /**
     * 평문 공개 — 운영에서는 추가 권한 필요. MVP: 멤버면 가능 + Audit.
     */
    @Transactional(readOnly = true)
    public ConfigDtos.SecretRevealResponse revealSecret(NimbusPrincipal principal, UUID secretId) {
        EnvSecret secret = secretRepository.findByIdAndDeletedAtIsNull(secretId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECRET_NOT_FOUND));
        requireEnvMember(principal, secret.getEnvironmentId());
        auditService.recordSuccess(
                principal, AuditAction.REVEAL_SECRET, "SECRET",
                secret.getId(), secret.getKey(), secret.getWorkspaceId(),
                "Secret value revealed"
        );
        return new ConfigDtos.SecretRevealResponse(
                secret.getId(),
                secret.getKey(),
                tokenCryptoService.decrypt(secret.getValueEnc()),
                secret.getRotationVersion()
        );
    }

    // ── helpers for promote copy ───────────────────────────

    @Transactional(readOnly = true)
    public List<EnvVariable> listVariableEntities(UUID environmentId) {
        return variableRepository.findByEnvironmentIdAndDeletedAtIsNullOrderByKeyAsc(environmentId);
    }

    @Transactional(readOnly = true)
    public List<EnvSecret> listSecretEntities(UUID environmentId) {
        return secretRepository.findByEnvironmentIdAndDeletedAtIsNullOrderByKeyAsc(environmentId);
    }

    @Transactional
    public int copyVariables(UUID sourceEnvId, ServiceEnvironment target, UUID actorId) {
        int count = 0;
        for (EnvVariable src : listVariableEntities(sourceEnvId)) {
            var existing = variableRepository.findByEnvironmentIdAndKeyAndDeletedAtIsNull(target.getId(), src.getKey());
            if (existing.isPresent()) {
                EnvVariable v = existing.get();
                v.updateValue(src.getValue());
                variableRepository.save(v);
            } else {
                variableRepository.save(EnvVariable.create(
                        target.getId(), target.getServiceId(), target.getWorkspaceId(),
                        src.getKey(), src.getValue(), actorId
                ));
            }
            count++;
        }
        return count;
    }

    @Transactional
    public int copySecrets(UUID sourceEnvId, ServiceEnvironment target, UUID actorId) {
        int count = 0;
        for (EnvSecret src : listSecretEntities(sourceEnvId)) {
            var existing = secretRepository.findByEnvironmentIdAndKeyAndDeletedAtIsNull(target.getId(), src.getKey());
            // ciphertext 그대로 복사 (동일 AES 키)
            if (existing.isPresent()) {
                EnvSecret s = existing.get();
                s.rotate(src.getValueEnc());
                secretRepository.save(s);
            } else {
                secretRepository.save(EnvSecret.create(
                        target.getId(), target.getServiceId(), target.getWorkspaceId(),
                        src.getKey(), src.getValueEnc(), actorId
                ));
            }
            count++;
        }
        return count;
    }

    private ServiceEnvironment requireEnvMember(NimbusPrincipal principal, UUID environmentId) {
        ServiceEnvironment env = environmentRepository.findByIdAndDeletedAtIsNull(environmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENVIRONMENT_NOT_FOUND));
        workspaceBootstrapService.requireMember(env.getWorkspaceId(), principal.userId());
        return env;
    }

    private ServiceEnvironment requireWritableEnv(NimbusPrincipal principal, UUID environmentId) {
        ServiceEnvironment env = requireEnvMember(principal, environmentId);
        workspacePermissionService.requireMutator(env.getWorkspaceId(), principal.userId());
        if (env.getStatus() == EnvironmentStatus.ARCHIVED) {
            throw new BusinessException(ErrorCode.ENVIRONMENT_ARCHIVED);
        }
        return env;
    }

    private ConfigDtos.VariableResponse toVariable(EnvVariable v) {
        return new ConfigDtos.VariableResponse(
                v.getId(), v.getEnvironmentId(), v.getKey(), v.getValue(),
                v.getCreatedAt(), v.getUpdatedAt()
        );
    }

    private ConfigDtos.SecretResponse toSecretMasked(EnvSecret s) {
        return new ConfigDtos.SecretResponse(
                s.getId(),
                s.getEnvironmentId(),
                s.getKey(),
                mask(s.getKey()),
                s.getRotationVersion(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                s.getLastSyncedAt(),
                s.getLastSyncStatus(),
                s.getLastSyncMessage()
        );
    }

    @Transactional(readOnly = true)
    public String decryptSecretValue(EnvSecret secret) {
        return tokenCryptoService.decrypt(secret.getValueEnc());
    }

    @Transactional
    public void markSecretSynced(EnvSecret secret, String status, String message) {
        secret.markSynced(status, message);
        secretRepository.save(secret);
    }

    static String mask(String key) {
        // 키 길이와 무관 고정 마스크
        return "••••••••";
    }
}
