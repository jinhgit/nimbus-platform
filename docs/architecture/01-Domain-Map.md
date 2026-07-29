# Domain Map

> 정본: [03-Canonical-Decisions.md](03-Canonical-Decisions.md)

## 1. Aggregate roots

| Aggregate | Parent | Notes |
|-----------|--------|--------|
| User | — | Identity, global role |
| Workspace | — | Collaboration root |
| Team | Workspace | Sub-group |
| Project | Workspace | Business context |
| Service | Project | **Deployable unit** |
| Environment | Service | Runtime / infra context |
| Template | Catalog | Golden Path asset |
| Wizard | Project/Service | Orchestration session |
| ProvisionJob | Wizard | Saga run |
| GitHubConnection | Workspace/User | SCM link |

---

## 2. Hierarchy

```text
User ──member──► Workspace
                   ├── Team / Member / Invitation
                   ├── Project
                   │     ├── Metadata (label, tag, annotation, favorite)
                   │     └── Service
                   │           ├── Repository (GitHub)
                   │           └── Environment
                   │                 ├── Variable / Secret
                   │                 ├── Deployment / Pipeline ref
                   │                 └── Incident
                   └── Catalog (templates)

Wizard ──► ProvisionJob ──► Steps (Saga)
```

---

## 3. Status machines

### Project

`CREATING → READY → ARCHIVED ⇄ READY → DELETING` · `FAILED`

### Service (목표 모델)

`CREATING → READY → FAILED · ARCHIVED`

### Environment

`CREATING → READY → DEPLOYING → FAILED · ARCHIVED`

### Deployment

`PENDING → RUNNING → SUCCESS · FAILED · ROLLBACK`

### Wizard

`DRAFT → VALIDATING → PROVISIONING → DEPLOYING → COMPLETED · FAILED · CANCELLED`

### Provision Job

`QUEUED → VALIDATING → GENERATING → PROVISIONING → DEPLOYING → VERIFYING → COMPLETED`  
· `FAILED · ROLLING_BACK · ROLLED_BACK · CANCELLED`

---

## 4. Domain events (주요)

```text
user.logged_in / logged_out
workspace.created / member.invited / member.joined
project.created / archived / cloned
environment.created / promoted
variable.created / secret.rotated / github.secret.synced
template.created / published
wizard.created / executed / completed / failed
provision.started / repository.created / helm.generated
argocd.created / deployment.completed / rollback.completed
recommendation.generated / architecture.reviewed
```

---

## 5. Workspace roles

| Capability | Owner | Admin | PE | Developer | Viewer |
|------------|:-----:|:-----:|:--:|:---------:|:------:|
| Workspace edit | ✅ | | | | |
| Invite | ✅ | ✅ | ✅ | | |
| Role change | ✅ | ✅ | | | |
| Project/Service create | ✅ | ✅ | ✅ | ✅ | |
| Deploy | ✅ | ✅ | ✅ | ✅ | |
| Monitoring | ✅ | ✅ | ✅ | ✅ | ✅ |

Global auth roles: `ADMIN · PLATFORM_ENGINEER · DEVELOPER · VIEWER`

---

## 6. Integrations

| System | Pattern |
|--------|---------|
| GitHub | `GitProvider` + retry + rate limit |
| Terraform | generate (MVP) / apply (v2) |
| Helm | template + values |
| ArgoCD | manifest + sync |
| LLM | `AIProvider` + Context + Guardrail |

---

## 7. Stores

| Store | Role |
|-------|------|
| PostgreSQL | Platform metadata |
| Redis | Session, cache, AI cache, locks |
| Git | App deploy source of truth |
| S3 + DynamoDB | TF remote state (cloud track) |
