# Round 104 — Legal AI Spine: research dossier and validation envelopes

## Entradas
- dossiê jurídico estruturado
- envelope de validação jurídica
- ampliação do facade/controller jurídico já existente

## Arquivos novos
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/LegalResearchDossierRequest.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/LegalResearchDossierResponse.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/LegalValidationRequest.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/LegalValidationResponse.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/spine/JuridicaResearchDossierService.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/spine/JuridicaValidationEnvelopeService.java`

## Arquivos alterados
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/spine/JuridicaSpineLabels.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/api/LegalAiController.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/intelligence/surface/LegalAiSurfaceFacadeService.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/v1/IAJuridicaV1.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/v2/IAJuridicaV2.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/v3/IAJuridicaV3.java`

## Resultado
- a spine jurídica agora expõe pipelines estruturados de pesquisa e validação dentro da IA já existente
- o controller jurídico ganhou rotas dedicadas para dossiê e validação
- o facade jurídico mantém a integração sem abrir outro sistema
