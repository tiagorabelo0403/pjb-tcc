# Round 126 — registry soberano de proveniência por evidência e anexo

## Objetivo
Endurecer a malha jurídica já existente para que trust zone, tool scope, approval e response composer passem a distinguir proveniência material de evidência e anexo antes de qualquer promoção para RAG, grounding, minuta, suggestion flow ou capability recovery.

## Entradas estruturais
- `LegalAiConversationEvidenceProvenanceService`
- `LegalAiConversationEvidenceProvenanceSnapshot`
- `LegalEvidenceTrustClassifier`
- `LegalAttachmentProvenanceClassifier`
- `LegalGroundingPromotionFence`
- `LegalDraftPromotionFence`
- `LegalEvidencePromotionPolicy`

## Integrações reais
- `LegalAiConversationOrchestrator`
  - resolve proveniência após trust zone
  - injeta `juridicaConversationEvidenceProvenance`
  - injeta `conversationEvidenceProvenance`
  - propaga razões de proveniência para `nextSteps`
- `LegalToolScopePolicy`
  - ganhou `enrichWithEvidenceProvenance(...)`
  - reflete status, tier, requisitos pendentes e tool ids bloqueados/step-up
- `LegalSensitiveActionApprovalService`
  - trata `EVIDENCE_PROVENANCE_*` como checkpoint material
  - endurece approval quando grounding/minuta/recovery não têm cadeia soberana segura
- `LegalAiConversationResponseComposerService`
  - expõe sinais de proveniência nos safeguards
  - narra estado da promoção para grounding/minuta/recovery

## Política soberana materializada
### Tier de evidência
- `OFFICIAL_DOCUMENT`
- `INSTITUTIONAL_CONTROLLED_DOCUMENT`
- `DERIVED_DOCUMENT`
- `UNTRUSTED_DOCUMENT`
- `NO_EVIDENCE`

### Regras centrais
- cadeia não confiável bloqueia grounding, minuta e capability recovery;
- cadeia derivada exige fronteira soberana e step-up antes de grounding ou minuta;
- cadeia institucional controlada pode seguir monitorada, mas não recebe a mesma promoção automática da cadeia oficial;
- ausência de âncora oficial em trust zone soberana gera requisito pendente de promoção.

## Evidência executável adicionada
- `LegalAiConversationEvidenceProvenanceServiceTest`
- `JuridicaLegalAiConversationRound126ArchitectureTest`
- ajuste em `JuridicaLegalAiConversationServiceTest`

## Validação honesta
- guards Python
- compilação dirigida com `javac`
- sem afirmar Maven global verde
- sem afirmar compile total do `pjb-api`
- sem afirmar Docker estável
