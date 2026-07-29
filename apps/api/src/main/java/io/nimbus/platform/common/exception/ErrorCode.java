package io.nimbus.platform.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INTERNAL_ERROR("COMMON001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("COMMON002", "Validation failed", HttpStatus.BAD_REQUEST),
    NOT_FOUND("COMMON003", "Resource not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("COMMON004", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON005", "Forbidden", HttpStatus.FORBIDDEN),
    CONFLICT("COMMON006", "Conflict", HttpStatus.CONFLICT),

    // Auth
    AUTH_OAUTH_FAILED("AUTH001", "GitHub OAuth Failed", HttpStatus.UNAUTHORIZED),
    AUTH_JWT_EXPIRED("AUTH002", "JWT expired", HttpStatus.UNAUTHORIZED),
    AUTH_REFRESH_EXPIRED("AUTH003", "Refresh token expired", HttpStatus.UNAUTHORIZED),
    AUTH_WORKSPACE_DENIED("AUTH004", "Workspace access denied", HttpStatus.FORBIDDEN),
    AUTH_USER_DISABLED("AUTH005", "User disabled", HttpStatus.FORBIDDEN),
    AUTH_INVALID_STATE("AUTH006", "Invalid OAuth state", HttpStatus.BAD_REQUEST),
    AUTH_SESSION_EXPIRED("AUTH007", "Session expired", HttpStatus.UNAUTHORIZED),
    AUTH_INVALID_SIGNATURE("AUTH008", "Invalid token signature", HttpStatus.UNAUTHORIZED),
    AUTH_DEV_DISABLED("AUTH009", "Dev login is disabled", HttpStatus.FORBIDDEN),
    AUTH_GITHUB_NOT_CONFIGURED("AUTH010", "GitHub OAuth is not configured", HttpStatus.SERVICE_UNAVAILABLE),

    // Workspace
    WORKSPACE_NOT_FOUND("WORKSPACE001", "Workspace not found", HttpStatus.NOT_FOUND),
    WORKSPACE_SLUG_DUPLICATE("WORKSPACE002", "Workspace slug already exists", HttpStatus.CONFLICT),
    WORKSPACE_PERMISSION("WORKSPACE003", "Workspace permission denied", HttpStatus.FORBIDDEN),
    WORKSPACE_LAST_OWNER("WORKSPACE004", "Cannot remove the last owner", HttpStatus.BAD_REQUEST),
    WORKSPACE_HAS_PROJECTS("WORKSPACE005", "Workspace still has projects", HttpStatus.BAD_REQUEST),
    MEMBER_ALREADY("MEMBER001", "Already a workspace member", HttpStatus.CONFLICT),
    MEMBER_INVITE_EXPIRED("MEMBER002", "Invitation expired", HttpStatus.BAD_REQUEST),
    MEMBER_NOT_FOUND("MEMBER003", "Member not found", HttpStatus.NOT_FOUND),
    TEAM_NOT_FOUND("TEAM001", "Team not found", HttpStatus.NOT_FOUND),
    TEAM_HAS_MEMBERS("TEAM002", "Team still has members", HttpStatus.BAD_REQUEST),

    // Project
    PROJECT_NAME_DUPLICATE("PROJECT001", "Project name already exists", HttpStatus.CONFLICT),
    PROJECT_NOT_FOUND("PROJECT002", "Project not found", HttpStatus.NOT_FOUND),
    PROJECT_PERMISSION("PROJECT003", "Project permission denied", HttpStatus.FORBIDDEN),
    PROJECT_ARCHIVED("PROJECT004", "Project is archived", HttpStatus.BAD_REQUEST),
    PROJECT_DELETE_BLOCKED("PROJECT005", "Project cannot be deleted", HttpStatus.BAD_REQUEST),

    // Catalog
    TEMPLATE_NOT_FOUND("TEMPLATE001", "Template not found", HttpStatus.NOT_FOUND),
    TEMPLATE_NAME_DUPLICATE("TEMPLATE002", "Template name already exists", HttpStatus.CONFLICT),
    TEMPLATE_NOT_PUBLISHED("TEMPLATE003", "Template is not published", HttpStatus.BAD_REQUEST),
    TEMPLATE_PUBLISH_FAILED("TEMPLATE004", "Template publish failed", HttpStatus.BAD_REQUEST),

    // Service
    SERVICE_NOT_FOUND("SERVICE001", "Service not found", HttpStatus.NOT_FOUND),
    SERVICE_NAME_DUPLICATE("SERVICE002", "Service name already exists", HttpStatus.CONFLICT),
    SERVICE_PERMISSION("SERVICE003", "Service permission denied", HttpStatus.FORBIDDEN),

    // Wizard
    WIZARD_NOT_FOUND("WIZARD001", "Wizard not found", HttpStatus.NOT_FOUND),
    WIZARD_INVALID_STATE("WIZARD002", "Wizard is not in a valid state", HttpStatus.BAD_REQUEST),
    WIZARD_VALIDATION_FAILED("WIZARD003", "Wizard validation failed", HttpStatus.BAD_REQUEST),
    WIZARD_ALREADY_EXECUTED("WIZARD004", "Wizard already executed", HttpStatus.CONFLICT),
    WIZARD_CANCEL_DENIED("WIZARD005", "Wizard cannot be cancelled", HttpStatus.BAD_REQUEST),

    // AI
    AI_RECOMMENDATION_FAILED("AI001", "AI recommendation failed", HttpStatus.BAD_GATEWAY);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
