"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { fetchMe, fetchServices, type AppService } from "@/lib/api";
import { IconWizard } from "@/components/icons";
import {
  Card,
  EmptyState,
  Page,
  PageHeader,
  StatusBadge,
} from "@/components/ui";

export default function ServicesPage() {
  const [services, setServices] = useState<AppService[]>([]);

  useEffect(() => {
    fetchMe().then(async (me) => {
      const ws = me.data?.workspace?.id;
      if (!ws) return;
      const res = await fetchServices({ workspaceId: ws });
      if (res.success && res.data) setServices(res.data);
    });
  }, []);

  return (
    <Page>
      <PageHeader
        eyebrow="Deploy"
        title="Services"
        description="Deploy units created via Wizard. Open a card for environments, pipelines, and detail."
        actions={
          <Link href="/wizard" className="nimbus-btn-primary">
            <IconWizard size={15} />
            Create Service
          </Link>
        }
      />

      <Card padding={false}>
        {services.length === 0 ? (
          <EmptyState
            title="No services yet"
            description="Use Service Wizard to provision a service with GitHub, K8s, and environments."
            action={
              <Link href="/wizard" className="nimbus-btn-primary">
                Open Wizard
              </Link>
            }
          />
        ) : (
          <ul className="divide-y divide-[var(--border)]">
            {services.map((s) => (
              <li key={s.id}>
                <Link
                  href={`/services/${s.id}`}
                  className="flex items-start justify-between gap-4 px-5 py-4 transition hover:bg-white/[0.03]"
                >
                  <div className="min-w-0">
                    <p className="font-medium text-zinc-100">{s.name}</p>
                    <p className="mt-1 text-xs text-[var(--muted)]">
                      {s.runtime} · {s.environmentType}
                      {s.databaseType ? ` · ${s.databaseType}` : ""}
                      {s.cacheType ? ` · ${s.cacheType}` : ""}
                    </p>
                    {s.githubRepoUrl ? (
                      <p className="mt-1 truncate text-[11px] text-sky-400/90">
                        {s.githubOwner}/{s.githubRepoName}
                      </p>
                    ) : null}
                  </div>
                  <div className="flex shrink-0 flex-col items-end gap-1.5">
                    <StatusBadge value={s.status} />
                    {s.k8sStatus ? <StatusBadge value={s.k8sStatus} /> : null}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </Card>
    </Page>
  );
}
