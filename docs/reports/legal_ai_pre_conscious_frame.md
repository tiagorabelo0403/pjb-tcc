# Legal AI Pre-Conscious Frame

## Escopo

A alteração adiciona uma moldura pré-consciente jurídica soberana ao fluxo conversacional existente, sem controller novo, sem rota nova e sem orquestrador paralelo.

## Componentes adicionados

- `LegalAiJuridicalLineageDescriptor`
- `LegalAiPreConsciousSignal`
- `LegalAiPreConsciousFrameSnapshot`
- `LegalAiJuridicalLineageRegistry`
- `LegalAiPreConsciousSignalExtractor`
- `LegalAiPreConsciousFrameService`
- `LegalAiPreConsciousToolScopeEnricher`

## Integração

O `LegalAiConversationOrchestrator` agora monta `preConsciousFrame` depois de trust zone, evidence provenance e knowledge coverage, mas antes do approval final. O tool scope final passa pelo `LegalAiPreConsciousToolScopeEnricher`, que pode manter, elevar para revisão assistida ou bloquear ferramentas.

## Metadados operacionais

- `preConsciousStatus`
- `preConsciousMode`
- `preConsciousAuthorityFloor`
- `preConsciousCognitivePosture`
- `preConsciousRiskScore`
- `preConsciousHumanReviewRequired`
- `preConsciousLearningCandidate`
- `preConsciousLineages`
- `preConsciousDominantLenses`
- `preConsciousAuthorityChecks`
- `preConsciousSignals`
- `preConsciousNextActions`

## Validação executada

- classes novas de produção compiladas com `javac --release 21` e stubs mínimos de anotação Spring;
- `LegalAiConversationResponseComposerService` compilado de forma dirigida com DTOs necessários;
- testes novos compilados com stubs mínimos de JUnit;
- probe local executado: `BLOCKED|SOVEREIGN_PRE_RESPONSE_LOCK|CITATION_FIRST_BLOCKING|100`;
- `architecture_hygiene_guard.py`: passou;
- `constructor_injection_guard.py`: passou;
- `runtime_concurrency_guard.py`: passou;
- `config_taxonomy_guard.py`: passou;
- `transactional_hotspot_guard.py --fail-on-missing-budgets`: passou.

## Limitação honesta

O Maven Wrapper não conseguiu baixar `apache-maven-3.9.6-bin.zip` no ambiente, portanto não há afirmação de Maven global verde.
