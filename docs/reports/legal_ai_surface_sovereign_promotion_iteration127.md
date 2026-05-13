# Round 127 — superfícies estruturadas com promoção soberana

## Objetivo
Levar a trust zone e a proveniência soberana já materializadas na conversa jurídica para as superfícies estruturadas de minuta e grounding.

## Entradas principais
- `LegalAiStructuredSurfaceGovernanceService`
- `LegalAiStructuredSurfaceGovernanceSnapshot`
- `LegalAiSurfaceFacadeService`
- `LegalDraftResponse`
- `LegalHallucinationGuardResponse`

## Resultado material
- `/api/ai/legal/minuta` não promove automaticamente cadeia derivada/institucional para redação mutável
- `/api/ai/legal/grounding/check` não trata cadeia soberana pendente como grounding materialmente alinhado
- scaffold governado determinístico quando a cerca soberana exige step-up
- bloqueio explícito quando a promoção documental é inviável

## Evidência
- teste unitário do serviço de governança de superfície
- teste arquitetural garantindo reuso da governança soberana na surface

## Honestidade
- sem afirmar build Maven global verde
- sem afirmar compile total do `pjb-api`
- sem afirmar Docker estável
- ZIP segue sem `.git`
