# Round 73 — Institutional workbench shell and action preview

## What entered

- `GET /api/v1/institucional/workbench`
- `GET /api/v1/institucional/workbench/action-preview`
- `InstitutionalWorkbenchService`
- `InstitutionalWorkbenchProfileResolver`
- `InstitutionalWorkbenchWidgetPolicyEngine`
- DTOs for profile, metrics, widgets, routes, workspace and action preview

## What this closes

- restores the missing base shell endpoint for the institutional workbench
- projects one canonical workspace for delegado, Ministério Público, Defensoria and Procuradoria
- keeps quick actions and operational queue wired to the same material guard already in use
- adds an explicit action preview endpoint for frontend CTA gating and explainability

## Runtime posture

- no new scheduler
- no session tracker
- no duplicated competence rule
- no second decision engine for institutional acts
- preview path reuses the same process object when already loaded, avoiding duplicate repository hits inside the shell service

## Main responses

- `InstitutionalWorkbenchWorkspaceResponse`
- `InstitutionalWorkbenchProfileResponse`
- `InstitutionalWorkbenchMetricResponse`
- `InstitutionalWorkbenchWidgetResponse`
- `InstitutionalWorkbenchRouteResponse`
- `InstitutionalWorkbenchActionPreviewResponse`

## Notes

- widget payloads were hardened to tolerate nullable values without exploding immutable map creation
- README updated with the new institutional shell and action preview surface
