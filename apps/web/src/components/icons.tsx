/**
 * Nimbus linear SVG icons (stroke only, currentColor).
 * Prefer these over filled/emoji icons in UI.
 */
import type { ReactNode, SVGProps } from "react";

export type IconProps = SVGProps<SVGSVGElement> & {
  size?: number;
};

function IconBase({
  size = 18,
  className,
  children,
  ...rest
}: IconProps & { children: ReactNode }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden
      {...rest}
    >
      {children}
    </svg>
  );
}

/** 대시보드 — 그리드 패널 */
export function IconDashboard(props: IconProps) {
  return (
    <IconBase {...props}>
      <rect x="3" y="3" width="7" height="9" rx="1.5" />
      <rect x="14" y="3" width="7" height="5" rx="1.5" />
      <rect x="14" y="12" width="7" height="9" rx="1.5" />
      <rect x="3" y="16" width="7" height="5" rx="1.5" />
    </IconBase>
  );
}

/** 프로젝트 — 폴더 */
export function IconProjects(props: IconProps) {
  return (
    <IconBase {...props}>
      <path d="M3 7.5A2.5 2.5 0 0 1 5.5 5H9l2 2h7.5A2.5 2.5 0 0 1 21 9.5v7A2.5 2.5 0 0 1 18.5 19h-13A2.5 2.5 0 0 1 3 16.5v-9Z" />
    </IconBase>
  );
}

/** 서비스 — 육각형 박스 */
export function IconServices(props: IconProps) {
  return (
    <IconBase {...props}>
      <path d="M12 2.5 20 7v10l-8 4.5L4 17V7l8-4.5Z" />
      <path d="M12 12v9.5" />
      <path d="M20 7 12 12 4 7" />
    </IconBase>
  );
}

/** 카탈로그 — 북마크/카탈로그 격자 */
export function IconCatalog(props: IconProps) {
  return (
    <IconBase {...props}>
      <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H18a2 2 0 0 1 2 2v14.5a.5.5 0 0 1-.8.4L16 17H6.5A2.5 2.5 0 0 0 4 19.5V5.5Z" />
      <path d="M8 8h6" />
      <path d="M8 12h4" />
    </IconBase>
  );
}

/** 서비스 생성 — 마법 지팡이 대신 명확한 생성(+) 레이어 */
export function IconWizard(props: IconProps) {
  return (
    <IconBase {...props}>
      <path d="M12 5v14" />
      <path d="M5 12h14" />
      <circle cx="12" cy="12" r="9" />
    </IconBase>
  );
}

/** 파이프라인 — 워크플로 노드 */
export function IconPipelines(props: IconProps) {
  return (
    <IconBase {...props}>
      <circle cx="6" cy="6" r="2.5" />
      <circle cx="18" cy="12" r="2.5" />
      <circle cx="6" cy="18" r="2.5" />
      <path d="M8.5 7.5 15.5 11" />
      <path d="M8.5 16.5 15.5 13" />
    </IconBase>
  );
}

/** 모니터링 — 활동 차트 */
export function IconMonitoring(props: IconProps) {
  return (
    <IconBase {...props}>
      <path d="M3 12h3l2.5-6 3 12 2.5-8L17 12h4" />
    </IconBase>
  );
}

/** 로그 — 문서 라인 */
export function IconLogs(props: IconProps) {
  return (
    <IconBase {...props}>
      <path d="M7 3.5h7l4 4V20a1.5 1.5 0 0 1-1.5 1.5H7A1.5 1.5 0 0 1 5.5 20V5A1.5 1.5 0 0 1 7 3.5Z" />
      <path d="M14 3.5V8h4" />
      <path d="M9 12h6" />
      <path d="M9 16h4" />
    </IconBase>
  );
}

/** 감사 로그 — 체크 클립보드 */
export function IconAudit(props: IconProps) {
  return (
    <IconBase {...props}>
      <path d="M9 4.5h6" />
      <path d="M10 3h4a1 1 0 0 1 1 1v1.5H9V4a1 1 0 0 1 1-1Z" />
      <rect x="5.5" y="5.5" width="13" height="15" rx="1.5" />
      <path d="m9 13 2 2 4-4" />
    </IconBase>
  );
}

/** 워크스페이스 — 사람들 */
export function IconWorkspaces(props: IconProps) {
  return (
    <IconBase {...props}>
      <circle cx="9" cy="8" r="3" />
      <path d="M3.5 19a5.5 5.5 0 0 1 11 0" />
      <circle cx="17" cy="9" r="2.5" />
      <path d="M16 19a4.5 4.5 0 0 1 4.5-4" />
    </IconBase>
  );
}

/** 인프라 — 서버 랙 */
export function IconInfrastructure(props: IconProps) {
  return (
    <IconBase {...props}>
      <rect x="4" y="3.5" width="16" height="6" rx="1.5" />
      <rect x="4" y="14.5" width="16" height="6" rx="1.5" />
      <path d="M8 6.5h.01" />
      <path d="M8 17.5h.01" />
      <path d="M12 6.5h4" />
      <path d="M12 17.5h4" />
    </IconBase>
  );
}

/** 설정 — 톱니 */
export function IconSettings(props: IconProps) {
  return (
    <IconBase {...props}>
      <circle cx="12" cy="12" r="3" />
      <path d="M12 3.5v2.2M12 18.3v2.2M4.9 6.5l1.6 1.6M17.5 15.9l1.6 1.6M3.5 12h2.2M18.3 12h2.2M4.9 17.5l1.6-1.6M17.5 8.1l1.6-1.6" />
    </IconBase>
  );
}

/** 로그아웃 */
export function IconLogout(props: IconProps) {
  return (
    <IconBase {...props}>
      <path d="M10 4.5H6.5A2 2 0 0 0 4.5 6.5v11a2 2 0 0 0 2 2H10" />
      <path d="M14 12h6.5" />
      <path d="m17.5 8.5 3 3.5-3 3.5" />
    </IconBase>
  );
}
