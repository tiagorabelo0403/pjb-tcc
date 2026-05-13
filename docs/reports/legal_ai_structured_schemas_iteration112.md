# Round 112 — Legal AI Structured Schemas

## Entregáveis
- catálogo canônico `LegalAiStructuredSchemaCatalog`
- definição estrutural `LegalAiSchemaDefinition`
- granularidade por campo com `LegalAiSchemaField`
- schemas concretos de triagem, checklist, plano processual, parecer, risco, draft envelope, despacho e decisão
- consumo do catálogo pelo `JuridicaStructuredOutputProfileService`
- recomendação de schema exposta na conversa e no dossiê

## Conexões materiais
- `LegalAiConversationContextAssemblerService` publica `juridicaStructuredSchemaCatalog` e `juridicaRecommendedSchema`
- `LegalAiConversationResponseComposerService` expõe `recommendedSchemaId`, `recommendedSchemaLabel` e `recommendedSchemaStage`
- `JuridicaResearchDossierService` adiciona `recommendedStructuredSchema` e `structuredSchemaCatalog` ao trace

## Evidência adicionada
- `LegalAiStructuredSchemaCatalogTest`
- `JuridicaLegalAiSpineRound112ArchitectureTest`
- ajustes em `JuridicaLegalAiSpineServiceTest`
- ajustes em `JuridicaResearchValidationPipelineTest`
- ajustes em `JuridicaLegalAiConversationServiceTest`

## Validação honesta
- guards Python: ok
- compilação dirigida do lote principal com `javac` e stubs transitórios: ok
- compilação dirigida dos testes do lote com `javac` e stubs transitórios: ok
- sem build Maven global verde
- sem compile total do `pjb-api`
- sem Docker estável
