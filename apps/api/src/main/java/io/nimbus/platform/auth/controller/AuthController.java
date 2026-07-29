package io.nimbus.platform.auth.controller;

import io.nimbus.platform.auth.dto.AuthDtos;
import io.nimbus.platform.auth.security.GithubProperties;
import io.nimbus.platform.auth.security.NimbusPrincipal;
import io.nimbus.platform.auth.security.SecurityUtils;
import io.nimbus.platform.auth.service.AuthService;
import io.nimbus.platform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final GithubProperties githubProperties;

    public AuthController(AuthService authService, GithubProperties githubProperties) {
        this.authService = authService;
        this.githubProperties = githubProperties;
    }

    @PostMapping("/dev-login")
    public ApiResponse<AuthDtos.LoginResponse> devLogin(
            @Valid @RequestBody AuthDtos.DevLoginRequest request,
            HttpServletResponse response
    ) {
        AuthDtos.LoginResponse login = authService.devLogin(request);
        writeRefreshCookie(response, login.refreshToken());
        return ApiResponse.ok(login);
    }

    @GetMapping("/github")
    public ApiResponse<AuthDtos.GithubLoginResponse> githubLogin() {
        return ApiResponse.ok(authService.startGithubLogin());
    }

    @GetMapping("/github/callback")
    public void githubCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response
    ) throws IOException {
        AuthDtos.LoginResponse login = authService.handleGithubCallback(code, state);
        writeRefreshCookie(response, login.refreshToken());
        String redirect = githubProperties.getFrontendCallback()
                + "?accessToken=" + URLEncoder.encode(login.accessToken(), StandardCharsets.UTF_8)
                + "&expiresIn=" + login.expiresIn();
        response.sendRedirect(redirect);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.LoginResponse> refresh(
            @CookieValue(value = "refresh_token", required = false) String cookieToken,
            @RequestBody(required = false) RefreshBody body,
            HttpServletResponse response
    ) {
        String refreshToken = cookieToken;
        if ((refreshToken == null || refreshToken.isBlank()) && body != null) {
            refreshToken = body.refreshToken();
        }
        AuthDtos.LoginResponse login = authService.refresh(refreshToken);
        writeRefreshCookie(response, login.refreshToken());
        return ApiResponse.ok(login);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        NimbusPrincipal principal = null;
        try {
            principal = SecurityUtils.requirePrincipal();
        } catch (Exception ignored) {
            // 토큰 만료 등으로 principal 없어도 로그아웃 처리
        }
        authService.logout(principal, refreshToken);
        clearRefreshCookie(response);
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.MeResponse> me() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(authService.me(principal));
    }

    @PatchMapping("/workspace")
    public ApiResponse<AuthDtos.LoginResponse> switchWorkspace(
            @Valid @RequestBody AuthDtos.WorkspaceSwitchRequest request,
            HttpServletResponse response
    ) {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        AuthDtos.LoginResponse login = authService.switchWorkspace(principal, request.workspaceId());
        writeRefreshCookie(response, login.refreshToken());
        return ApiResponse.ok(login);
    }

    @PostMapping("/validate")
    public ApiResponse<AuthDtos.TokenValidateResponse> validate() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(authService.validate(principal));
    }

    @GetMapping("/permissions")
    public ApiResponse<AuthDtos.PermissionsResponse> permissions() {
        NimbusPrincipal principal = SecurityUtils.requirePrincipal();
        return ApiResponse.ok(authService.permissions(principal));
    }

    private void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record RefreshBody(String refreshToken) {
    }
}
