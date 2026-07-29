"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import {
  fetchCatalogTemplate,
  type CatalogTemplateDetail,
} from "@/lib/api";
import {
  Card,
  EmptyState,
  ErrorBanner,
  LoadingBlock,
  Page,
  PageHeader,
  StatusBadge,
} from "@/components/ui";

type Tab = "blueprint" | "helm" | "terraform" | "workflow";

export default function CatalogDetailPage() {
  const params = useParams();
  const templateId = String(params.templateId ?? "");
  const [item, setItem] = useState<CatalogTemplateDetail | null>(null);
  const [tab, setTab] = useState<Tab>("blueprint");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!templateId) return;
    setLoading(true);
    fetchCatalogTemplate(templateId).then((res) => {
      setLoading(false);
      if (!res.success || !res.data) {
        setError(res.error?.message ?? "템플릿을 찾을 수 없습니다.");
        setItem(null);
        return;
      }
      setItem(res.data);
      setError(null);
    });
  }, [templateId]);

  if (loading) {
    return (
      <Page>
        <PageHeader eyebrow="Catalog" title="Template" />
        <LoadingBlock label="템플릿을 불러오는 중…" />
      </Page>
    );
  }

  if (!item) {
    return (
      <Page>
        <PageHeader eyebrow="Catalog" title="Template" />
        {error ? <ErrorBanner message={error} /> : null}
        <EmptyState
          title="템플릿 없음"
          description="목록으로 돌아가 다시 선택하세요."
          action={
            <Link href="/catalog" className="nimbus-btn-primary">
              Catalog
            </Link>
          }
        />
      </Page>
    );
  }

  const content =
    tab === "blueprint"
      ? item.blueprint
      : tab === "helm"
        ? item.defaultHelmValues
        : tab === "terraform"
          ? item.defaultTerraformVars
          : item.defaultWorkflow;

  return (
    <Page>
      <PageHeader
        eyebrow="Catalog"
        title={item.name}
        description={item.description ?? "Golden Path 템플릿 상세"}
        actions={
          <div className="flex flex-wrap gap-2">
            <Link href="/catalog" className="nimbus-btn-ghost">
              목록
            </Link>
            <Link
              href={`/wizard?templateId=${item.id}`}
              className="nimbus-btn-primary"
            >
              Wizard로 사용
            </Link>
          </div>
        }
      />

      <div className="mb-5 flex flex-wrap gap-2 text-xs">
        <StatusBadge value={item.status} />
        <span className="rounded border border-[var(--border)] px-2 py-0.5 text-[var(--muted)]">
          {item.runtime}
        </span>
        <span className="rounded border border-[var(--border)] px-2 py-0.5 text-[var(--muted)]">
          {item.type}
        </span>
        <span className="rounded border border-[var(--border)] px-2 py-0.5 text-[var(--muted)]">
          {item.language}
        </span>
        <span className="rounded border border-[var(--border)] px-2 py-0.5 text-[var(--muted)]">
          v{item.latestVersion}
        </span>
        {item.official ? (
          <span className="rounded-full bg-[var(--primary)]/20 px-2 py-0.5 text-[var(--primary)]">
            Official
          </span>
        ) : null}
        {item.tags ? (
          <span className="text-[var(--muted)]">{item.tags}</span>
        ) : null}
      </div>

      <Card>
        <div className="mb-3 flex flex-wrap gap-2">
          {(
            [
              ["blueprint", "Blueprint"],
              ["helm", "Helm"],
              ["terraform", "Terraform"],
              ["workflow", "Actions"],
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              onClick={() => setTab(id)}
              className={`rounded-full px-3 py-1 text-xs font-medium ${
                tab === id
                  ? "bg-[var(--primary)] text-white"
                  : "border border-[var(--border)] text-[var(--muted)] hover:text-white"
              }`}
            >
              {label}
            </button>
          ))}
        </div>
        <pre className="max-h-[28rem] overflow-auto whitespace-pre-wrap rounded-lg border border-[var(--border)] bg-black/30 p-4 font-mono text-[11px] leading-relaxed text-zinc-300">
          {content?.trim() ? content : "이 탭에 미리보기가 없습니다."}
        </pre>
      </Card>
    </Page>
  );
}
