# Small class continuation review

## Objective
Continue the refinement of very small classes without inflating DTOs, exceptions or declarative Spring configurations.

## Classes materially improved
- `platform/logging/MdcTraceScope`
  - replaced no-op scope with real MDC capture/restore
  - trims values and removes blank keys instead of leaking stale MDC state
- `core/kernel/recursal/RecursalHash`
  - delegates to the canonical SHA-256 utility
  - rejects null/blank input early
- `service/PdfGeneratorService`
  - replaced fake random hash with deterministic SHA-256 over HTML + canonical metadata
  - keeps URL generation stable by document id
- `integration/govbr/mock/GovBrMockSignatureService`
  - enforces document id presence
  - replaced raw random UUID with deterministic mock flow fingerprint
- `core/frontend/app/domain/PjbFrontendMenuItemView`
  - normalization and navigation/step-up helpers
- `core/frontend/delivery/domain/PjbFrontendRouteView`
  - normalization and route identity helpers
- `core/comunicacao/institucional/panel/domain/InstitutionalPanelCard`
  - normalization, non-negative counters and action/trend helpers
- `platform/runtime/domain/PjbRuntimeMemoryBudgetView`
  - non-negative budgets and baseline/headroom helpers
- `core/processo/prova/domain/ProcessoProvaIntegridade`
  - normalized hashes and integrity/cross-case helpers
- `service/processual/calculo/CalculoJudicialApiRouteContext`
  - normalization and active/legacy-gap helpers
- `service/processual/calculo/CalculoJudicialGeracaoContext`
  - normalization and audit/equipe helpers
- `service/oficial_justica/OficialJusticaAgendaTerritorialHint`
  - normalization and confidence clamping

## Small classes intentionally kept small
The following categories were not inflated because short size is legitimate:
- DTO request/response carriers
- marker exceptions
- repository interfaces
- Spring configuration classes that only bind one concern
- enums and records that are already semantically complete

## Validation
- `scripts/import_sanity_probe.py`: OK
- `scripts/repository_layout_guard.py`: OK
- `scripts/runtime_concurrency_guard.py`: OK
