# Round 132 — Boundary HTTP da conversa jurídica única

## Objetivo
Levar `trust zone`, `evidence provenance` e `approval` da conversa jurídica única até o boundary HTTP de `/api/ai/legal/conversation`, com prova executável por provider contract e integration test.

## O que entrou
- Ampliação de `LegalAiControllerProviderContractTest` para três estados de conversa soberana.
- Ampliação de `LegalAiControllerIT` para validar `approvalStatus`, `trustZoneStatus`, `trustZone`, `evidenceProvenanceStatus` e `evidenceProvenanceTier`.
- Ampliação do pacto `PjbLegalAiConsumer-PjbLegalAiProvider.json` com interações da conversa jurídica.
- Novo guard `JuridicaLegalAiConversationRound132ArchitectureTest`.
- Inclusão de `/api/ai/legal/conversation` na faixa `legal-ai-governed-surfaces`.

## Estados provados
- `AUTO_READONLY` + `PUBLIC` + `OFFICIAL_DOCUMENT`
- `STEP_UP_REQUIRED` + `SIGILOSA` + `DERIVED_DOCUMENT`
- `HUMAN_REVIEW_REQUIRED` + `CRITICAL` + `UNTRUSTED_DOCUMENT`

## Validação honesta
- Guards Python passaram.
- Compilação dirigida dos testes e guards alterados passou.
- Não houve build Maven global verde.
- Não houve compile total do `pjb-api`.
