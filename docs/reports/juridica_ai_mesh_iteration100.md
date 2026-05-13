# Round 100 — malha unificada da IA jurídica

## Entradas principais
- catálogo canônico de ferramentas jurídicas
- perfil unificado da malha jurídica
- runtime governado por `PjbVirtualThreadSpine` e `PjbExecutionOrchestrator`
- integração do perfil em V1, V2, V3, mesh, skills e surfaces jurídicas

## Eixos materializados
- RAG híbrido jurídico
- MCP jurídico read-only por padrão
- memória por processo/perfil/sessão
- filtros de qualidade e segurança compartilhados
- profundidade jurídica citation-first

## Arquivos centrais
- `model/dto/ai/legal/mesh/LegalAiToolDescriptor.java`
- `model/dto/ai/legal/mesh/LegalAiMeshProfileResponse.java`
- `ai/juridica/mesh/JuridicaMeshLabels.java`
- `ai/juridica/mesh/JuridicaLegalToolCatalogService.java`
- `ai/juridica/mesh/JuridicaUnifiedMeshProfileService.java`
- `ai/juridica/api/JuridicaMeshProfileController.java`
- `ai/juridica/policy/JuridicaAdaptiveMeshGovernanceService.java`
- `ai/juridica/v1/IAJuridicaV1.java`
- `ai/juridica/v2/IAJuridicaV2.java`
- `ai/juridica/v3/IAJuridicaV3.java`
- `legal/skills/api/v58/LegalSkillsControllerV58.java`
- `service/intelligence/surface/LegalAiSurfaceFacadeService.java`

## Validação honesta
- compilação dirigida do núcleo novo passou
- `runtime_concurrency_guard.py` passou
- sem afirmar build Maven global verde
- sem afirmar compile total do `pjb-api`
