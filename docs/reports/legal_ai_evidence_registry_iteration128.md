# Round 128 — registry material de evidência soberana

## Objetivo
Materializar a cadeia soberana por evidência e anexo já classificada nos rounds 125 a 127, sem criar eixo paralelo, para que a promoção para `RAG`, `grounding`, `minuta`, `suggestion flow` e `capability recovery` passe a ter âncoras explícitas e auditáveis.

## O que entrou
- `LegalAiConversationEvidenceDescriptor`
- `LegalEvidenceSovereignRegistryService`
- enriquecimento do `LegalAiConversationEvidenceProvenanceSnapshot` com:
  - descritores materiais
  - evidências promovidas por fluxo
- propagação para:
  - `LegalToolScopePolicy`
  - `LegalAiStructuredSurfaceGovernanceSnapshot`
  - `LegalAiSurfaceFacadeService`
  - `LegalAiConversationResponseComposerService`

## Efeito material
- evidência oficial promovida agora fica identificável por ID e hash
- evidência derivada ou não confiável continua visível, mas não sobe como âncora promovida
- a surface de minuta recebe explicitamente as evidências promovidas para a redação
- a trilha final da conversa passa a expor os descritores materiais da cadeia soberana

## Robustez
- sem novo executor
- sem nova fila
- sem scheduler paralelo
- sem módulo satélite
- sem strings institucionais soltas fora do eixo existente
- correção defensiva no `enrichSafeguards(...)` para evitar `Map.copyOf(...)` com `null`

## Testes adicionados ou reforçados
- `LegalEvidenceSovereignRegistryServiceTest`
- `LegalAiConversationEvidenceProvenanceServiceTest`
- `LegalAiStructuredSurfaceGovernanceServiceTest`
- `JuridicaLegalAiSurfaceRound128ArchitectureTest`

## Validação honesta
- `runtime_concurrency_guard.py`: passou
- `architecture_hygiene_guard.py`: passou
- `constructor_injection_guard.py`: passou
- `config_taxonomy_guard.py`: passou
- `transactional_hotspot_guard.py --fail-on-missing-budgets`: passou
- `repository_layout_guard.py`: passou
- compilação dirigida do lote principal alterado com `javac` e stubs transitórios locais: passou
- compilação dirigida dos testes novos/reforçados com `javac` e stubs transitórios locais: passou
- sem afirmar Maven global verde
- sem afirmar compile total do `pjb-api`
- sem afirmar Docker estável
- sem validação Git real porque o ZIP continua sem `.git`
