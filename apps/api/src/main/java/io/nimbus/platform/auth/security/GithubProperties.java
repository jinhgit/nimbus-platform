package io.nimbus.platform.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nimbus.github")
public class GithubProperties {
    private String clientId = "";
    private String clientSecret = "";
    /** 로그인용 OAuth callback */
    private String redirectUri = "http://localhost:8080/api/v1/auth/github/callback";
    private String frontendCallback = "http://localhost:3000/auth/callback";
    /** SCM 연결용 OAuth callback (repo scope) */
    private String scmRedirectUri = "http://localhost:8080/api/v1/github/oauth/callback";
    private String scmFrontendCallback = "http://localhost:3000/settings";
    private String scmScopes = "repo workflow read:user user:email";

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getFrontendCallback() {
        return frontendCallback;
    }

    public void setFrontendCallback(String frontendCallback) {
        this.frontendCallback = frontendCallback;
    }

    public String getScmRedirectUri() {
        return scmRedirectUri;
    }

    public void setScmRedirectUri(String scmRedirectUri) {
        this.scmRedirectUri = scmRedirectUri;
    }

    public String getScmFrontendCallback() {
        return scmFrontendCallback;
    }

    public void setScmFrontendCallback(String scmFrontendCallback) {
        this.scmFrontendCallback = scmFrontendCallback;
    }

    public String getScmScopes() {
        return scmScopes;
    }

    public void setScmScopes(String scmScopes) {
        this.scmScopes = scmScopes;
    }
}
