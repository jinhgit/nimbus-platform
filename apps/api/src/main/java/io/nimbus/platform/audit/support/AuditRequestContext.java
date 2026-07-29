package io.nimbus.platform.audit.support;

/**
 * 요청 단위 IP / User-Agent 보관 (Filter 가 설정).
 */
public final class AuditRequestContext {

    private static final ThreadLocal<String> IP = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_AGENT = new ThreadLocal<>();

    private AuditRequestContext() {
    }

    public static void set(String ipAddress, String userAgent) {
        IP.set(ipAddress);
        USER_AGENT.set(userAgent);
    }

    public static String ipAddress() {
        return IP.get();
    }

    public static String userAgent() {
        return USER_AGENT.get();
    }

    public static void clear() {
        IP.remove();
        USER_AGENT.remove();
    }
}
