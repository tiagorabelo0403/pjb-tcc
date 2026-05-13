# Round 135 — política seletiva de origem assinada por capability na conversa jurídica

## Objetivo
Endurecer a borda de `/api/ai/legal/conversation` para permitir browser governado em capability assistiva e exigir `signed attestation` apenas quando a capability jurídica pedida for sensível.

## Materialização
- `ApiRequestOriginSelectiveRequirementService`
- `ApiRequestOriginGovernanceProperties.selectiveSignedRules`
- `ApiRequestOriginGovernanceFilter` com:
  - `X-PJB-Origin-Requirement`
  - `X-PJB-Origin-Capability`
  - rejeição `signed_attestation_required_for_capability`
- `application.yml` com regra seletiva para `conversation`

## Prova executável
- `LegalAiSelectiveSignedOriginGovernanceIT`
- `LegalAiSelectiveSignedOriginProviderContractTest`
- `PjbLegalAiSelectiveSignedOriginConsumer-PjbLegalAiSelectiveSignedOriginProvider.json`
- `JuridicaLegalAiSelectiveSignedOriginRound135ArchitectureTest`
- `PjbLegalAiSelectiveSignedOriginContractCoverageArchitectureTest`

## Cenários cobertos
1. Browser governado aceito para `LEGAL_GENERAL_ASSIST_V3`
2. Browser governado bloqueado para `LEGAL_DRAFT_V2`
3. Canal assinado aceito para `LEGAL_DRAFT_V2`

## Observação honesta
A validação desta rodada foi fechada com guards Python, compilação dirigida do lote alterado e dos testes novos, mais revisão estrutural do pacto e do YAML. Não há afirmação de build Maven global verde, compile total do `pjb-api` ou Docker estável.
