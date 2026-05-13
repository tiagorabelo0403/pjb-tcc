# Round 123 - Evidência executável em consulta pública

## Objetivo
Fechar parte do delta entre superfície pública declarada e evidência executável real no bounded context de consulta pública, sem regredir sigilo, budgets transacionais ou filtros de exposição documental.

## O que entrou
- budgets transacionais explícitos para leituras públicas centrais:
  - `ConsultaPublicaSearchService.searchPublic`
  - `ConsultaPublicaSearchService.resolvePublicPage`
  - `ConsultaPublicaWorkspaceService.workspace`
  - `ConsultaPublicaWorkspaceService.detail`
  - `PublicProcessoConsultaService.consultarPorNumero`
- `ConsultaPublicaSearchFlowIT`
  - comprova busca pública real em Postgres/Testcontainers
  - comprova exclusão de autos não públicos na consulta textual
  - comprova resolução de página pública apenas para ato judicial efetivamente público
  - comprova bloqueio de peça pública que não é despacho/decisão/sentença/acórdão
- `PublicProcessoConsultaFlowIT`
  - comprova resumo público de processo com limitação de movimentações
  - comprova mascaramento de processo que exige credencial
  - comprova ocultação total de processo com restrição máxima
- workflow `quality-gates.yml` ampliado para incluir os novos fluxos de integração

## Risco fechado nesta rodada
A camada de consulta pública já tinha controllers, DTOs e testes unitários locais, mas ainda faltava evidência executável suficiente de que:

- a busca textual não mistura processo público com processo restrito
- a navegação por página não abre peça errada sob aparência de documento público
- o resumo público não vaza partes/movimentações em processos que exigem credencial
- processos com restrição máxima não aparecem nem como resumo parcial

## Limitação honesta
O ambiente continua sem permitir validação Maven completa via wrapper por falha externa de download do Maven. Nesta rodada a validação prática permanece ancorada em:

- testes adicionados no código
- guards Python
- inspeção estrutural
- `git diff --check`
- commit local temporário

## Próximo alvo recomendado
- ampliar Testcontainers para ajuizamento e process lifecycle
- ampliar Pact provider verification para search/detail da consulta pública
- continuar decomposição de hotspots remanescentes com forte evidência de teste ao redor
