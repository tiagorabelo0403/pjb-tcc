# Round 28 — recuperação de contracts e bordas HTTP

## Correções materiais

- Provider contracts Spring 6 migrados para `PactVerificationSpring6Provider` quando usam `Spring6MockMvcTestTarget`.
- Interações assinadas de Legal AI passaram a injetar `remoteAddr` confiável no `MockHttpServletRequestBuilder`.
- Pacts de erro da borda Legal AI foram alinhados para `application/problem+json`.
- `LegalAiConversationResponse` passou a expor aliases de compatibilidade para `version`, `capability` e `trace` sem quebrar os campos canônicos atuais.
- `AjuizamentoService` deixou de depender diretamente de `NationalProceduralRoutingService`; a consolidação procedimental foi movida para `AjuizamentoProceduralContextService`.
- Scanner de rotas passou a resolver constantes públicas de rota fora de `OperationalApiRoutes`.
- `FazendaTributarioCalculoAvancadoRequest` passou a aceitar `dataVencimento` como alias seguro de `vencimento`.
- Contexto de certificado qualificado passou a normalizar campos nulos antes de classificação institucional.
- `.mvn/maven.config` voltou a declarar `-Dmaven.compiler.release=21`.

## Validação executada

- `scripts/architecture_hygiene_guard.py`: OK
- `scripts/constructor_injection_guard.py`: OK
- `scripts/runtime_concurrency_guard.py`: OK
- `scripts/transactional_hotspot_guard.py --fail-on-missing-budgets`: OK
- `scripts/config_taxonomy_guard.py`: OK
- `scripts/repository_layout_guard.py`: OK
- compilação isolada do `ControllerRouteGovernanceScanner` com JDK 21: OK

## Limitação honesta

A execução Maven dirigida não foi concluída neste ambiente porque o Maven Wrapper não conseguiu baixar `apache-maven-3.9.6-bin.zip` a partir do repositório Maven Central.
