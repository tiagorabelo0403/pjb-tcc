# Platform Structural Decomposition — Round 109

## Escopo

Rodada focada em continuar a resposta ao diagnóstico estrutural com ênfase em:

- redução de concentração na `RecursalPeticionamentoFacadeService`
- deslocamento de drafting e projections para assemblers especializados
- trava arquitetural de regressão por construtor/dependência
- testes dedicados para os novos recortes

## Mudanças realizadas

### 1. Decomposição da facade recursal

A `RecursalPeticionamentoFacadeService` deixou de concentrar:

- montagem de minuta assistida
- carry-over decisório documental
- projections de estratégia, workspace, endpoints e grafo
- enriquecimento de sigilo da superfície recursal

Novos componentes:

- `RecursalDraftPreviewAssembler`
- `RecursalProjectionAssembler`

Resultado objetivo após a rodada:

- `RecursalPeticionamentoFacadeService`: **1496 -> 1075 linhas**
- construtor da facade: **18 -> 17 dependências**
- dependências especializadas removidas da facade:
  - `LegalDraftingService`
  - `DocumentoProcessualRepository`
  - `DocumentoPaginaRepository`

### 2. Testes adicionados

- `RecursalDraftPreviewAssemblerTest`
- `RecursalProjectionAssemblerTest`
- `PjbHotspotRefinementArchitectureTest`

Esses testes cobrem:

- fallback seguro da minuta quando o motor de drafting não retorna conteúdo útil
- substituição do placeholder recursal por razões reais
- inferência de corte superior em ausência de plano/admissibilidade
- enriquecimento de sigilo em estratégia/workspace
- garantia arquitetural de que a facade delega drafting e carry-over aos assemblers dedicados

### 3. Guard novo de construtor/dependência

Novo script:

- `scripts/constructor_injection_guard.py`

Saídas geradas:

- `docs/reports/constructor_injection_guard.json`
- `docs/reports/constructor_injection_guard.md`

Objetivo:

- medir concentração por construtor
- destacar services/facades/engines com excesso de dependências
- dar visibilidade contínua aos hotspots de SRP e decomposição

### 4. Pipeline

`quality-gates.yml` atualizado para executar:

- `constructor_injection_guard.py`
- `PjbHotspotRefinementArchitectureTest`
- `RecursalDraftPreviewAssemblerTest`
- `RecursalProjectionAssemblerTest`

## Observações

Ainda existem hotspots severos no código-base, inclusive com mais de 30 dependências injetadas em classes de produção. Esta rodada não encerra o problema; ela abre um padrão reproduzível e verificável para as próximas extrações.

## Próxima frente recomendada

Atacar a próxima classe crítica com melhor relação risco/retorno:

- `QualifiedSignatureIdentityContextService`
- `ProceduralCatalogSupport`
- `SecretariatQueueQueryService`
- `OficialJusticaPainelService`

Prioridade técnica sugerida: classes acima de 900 linhas com 12+ dependências ou classes com 16+ dependências totais no construtor.
