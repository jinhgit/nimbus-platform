# Nimbus Platform — Domain Map

## 1. Aggregate Roots

| Aggregate | Parent | Responsibility |
|-----------|--------|----------------|
| User | — | Identity, Role |
| Workspace | — | Collaboration root |
| Team | Workspace | Sub-group |
| Project | Workspace | Business context |
| Service | Project | Deployable unit |
| Environment | Service | Runtime / Infra context |
| Template (Catalog) | Workspace/Global | Golden Path asset |
| Wizard | Project | Orchestration session |
| ProvisionJob | Wizard | Saga execution |
| GitHubConnection | Workspace/User | SCM link |

---

## 2. Hierarchy

```text
User
  └── Workspace (member)
        ├── Team
        │     └── Member
        ├── Project
        │     ├── Metadata (Label, Tag, Annotation, Favorite)
        │     └── Service
        │           ├── Environment (DEV / STAGE / PRODUCTION)
        │           │     ├── Variable / Secret
        │           │     ├── Deployment
        │           │     └── Incident
        │           └── Repository (GitHub)
        ├── Catalog / Template
        └── AI Review / Prompt History

Wizard ──► ProvisionJob ──► Steps (Saga)
```

---

## 3. Status Machines (요약)

### Project

`CREATING → READY → ARCHIVED → (RESTORED→READY) → DELETING · FAILED`

### Environment

`CREATING → READY → DEPLOYING → FAILED · ARCHIVED`

### Deployment

`PENDING → RUNNING → SUCCESS · FAILED · ROLLBACK`

### Wizard

`DRAFT → VALIDATING → PROVISIONING → DEPLOYING → COMPLETED · FAILED · CANCELLED`

### Provision Job

`QUEUED → VALIDATING → GENERATING → PROVISIONING → DEPLOYING → VERIFYING → COMPLETED · FAILED · ROLLING_BACK · ROLLED_BACK · CANCELLED`

---

## 4. Major Domain Events

```text
user.logged_in / user.logged_out / workspace.changed
workspace.created / member.invited / member.joined
project.created / project.archived / project.cloned
environment.created / environment.promoted
variable.created / secret.rotated / github.secret.synced
template.created / template.published
wizard.created / wizard.executed / wizard.completed
provision.started / repository.created / helm.generated
argocd.created / deployment.completed / rollback.completed
recommendation.generated / architecture.reviewed
```

---

## 5. Role Matrix (Workspace)

| Capability | Owner | Admin | Platform Engineer | Developer | Viewer |
|------------|:-----:|:-----:|:-----------------:|:---------:|:------:|
| Workspace edit | ✅ | ❌ | ❌ | ❌ | ❌ |
| Invite member | ✅ | ✅ | ✅ | ❌ | ❌ |
| Role change | ✅ | ✅ | ❌ | ❌ | ❌ |
| Project create | ✅ | ✅ | ✅ | ✅ | ❌ |
| Deploy | ✅ | ✅ | ✅ | ✅ | ❌ |
| Monitoring | ✅ | ✅ | ✅ | ✅ | ✅ |

Global roles (Auth): `ADMIN · PLATFORM_ENGINEER · DEVELOPER · VIEWER`

---

## 6. Integration Boundaries

| System | Access Pattern | Abstraction |
|--------|----------------|-------------|
| GitHub | Adapter + Retry + Rate Limit | `GitProvider` |
| Terraform | Generate files (MVP) / apply (v2) | Provision Step |
| Helm | Template + values | Provision Step |
| ArgoCD | Manifest + Sync | Provision Step |
| Kubernetes | API / GitOps | Env + Deploy |
| LLM | AIProvider + Guardrail | Decision Engine |

---

## 7. Data Stores

| Store | Use |
|-------|-----|
| PostgreSQL 16 | Platform metadata (not live K8s objects as SoT) |
| Redis | Session, AI cache, job locks, OAuth state |
| S3 + DynamoDB | Terraform remote state (infra track) |
| Git | GitOps source of truth for app deploy |
