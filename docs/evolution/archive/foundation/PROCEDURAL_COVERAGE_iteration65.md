# Round 65 - Cobertura nacional de ritos e direitos

## O que entrou
- malha nacional tipada de cobertura de ritos processuais e garantias essenciais
- superfície administrativa `/api/v1/admin/scale-architecture/judicial-procedural-coverage`
- detalhe administrativo por rito `/api/v1/admin/scale-architecture/judicial-procedural-coverage/{rito}`
- superfície processual unificada `/api/v1/processual/unificado/cobertura-ritos-direitos`
- detalhe processual por rito `/api/v1/processual/unificado/cobertura-ritos-direitos/{rito}`
- flags adicionais de `supportsAllBrazilianRights` e `supportsAllProceduralGuarantees` nas trilhas de leitura documental e metadata procedural

## Núcleo novo
- `NationalProceduralRightsCoverageService`
- `NationalProceduralRightsCatalogSupport`
- `NationalProceduralRightsCoverageSnapshot`
- `NationalProceduralRightsCoverageFamily`
- `NationalProceduralRightsCoverageRow`

## Efeito prático
- o PJB passa a expor catálogo vivo de cobertura por rito, ramo, grupo, justiça provável, garantias essenciais, checkpoints operacionais e marcadores institucionais
- a malha processual unificada deixa explícita a pretensão de cobertura nacional de ritos e direitos, sem depender apenas de enum solta ou claim genérica no frontend
