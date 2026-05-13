# Round 109 — Motor simbólico real na validação jurídica

## Escopo
Materialização do primeiro lote executável do motor simbólico jurídico dentro da IA já existente, sem módulo paralelo e sem espelhamento de regras fora da espinha atual.

## Artefatos novos
- `ai/juridica/symbolic/LegalSymbolicValidationCatalog.java`
- `ai/juridica/symbolic/LegalSymbolicValidationContext.java`
- `ai/juridica/symbolic/LegalSymbolicValidationIssue.java`
- `ai/juridica/symbolic/LegalSymbolicValidationOutcome.java`
- `ai/juridica/symbolic/LegalSymbolicValidationExecution.java`
- `ai/juridica/symbolic/LegalDeterministicRuleEngine.java`
- `ai/juridica/symbolic/LegalPrazoRuleEngine.java`
- `ai/juridica/symbolic/LegalCompetenciaRuleEngine.java`
- `ai/juridica/symbolic/LegalCabimentoRuleEngine.java`
- `ai/juridica/symbolic/LegalSigiloRuleEngine.java`
- `ai/juridica/symbolic/LegalProceduralCompatibilityEngine.java`
- `ai/juridica/symbolic/JuridicaSymbolicValidationExecutionService.java`

## Artefatos alterados
- `ai/juridica/spine/JuridicaSymbolicValidationProfileService.java`
- `ai/juridica/spine/JuridicaValidationEnvelopeService.java`
- `ai/juridica/conversation/LegalAiConversationResponseComposerService.java`
- `README.md`

## Materialização
- execução real de engines determinísticas por `PRAZO`, `COMPETENCIA`, `CABIMENTO`, `SIGILO` e `PROCEDURAL_COMPATIBILITY`;
- agregação central de outcomes, contradições e missing evidence;
- `trace` enriquecido com status e outcomes da execução simbólica;
- catálogo central dos códigos dos engines para evitar strings espalhadas.

## Evidência executável adicionada
- `JuridicaSymbolicValidationExecutionServiceTest`
- `JuridicaLegalAiSpineRound109ArchitectureTest`
- atualização de `JuridicaResearchValidationPipelineTest`
- atualização de `JuridicaLegalAiConversationServiceTest`

## Validação honesta
- `runtime_concurrency_guard.py`: passou
- `architecture_hygiene_guard.py`: passou
- `constructor_injection_guard.py`: passou
- `config_taxonomy_guard.py`: passou
- `transactional_hotspot_guard.py --fail-on-missing-budgets`: passou
- compilação dirigida do lote alterado com `javac`: passou
- probe local do executor simbólico: bloqueio confirmado para juizado com escalada recursal incompatível
- sem afirmar build Maven global verde
- sem afirmar compile total do `pjb-api`
- sem afirmar Docker estável
