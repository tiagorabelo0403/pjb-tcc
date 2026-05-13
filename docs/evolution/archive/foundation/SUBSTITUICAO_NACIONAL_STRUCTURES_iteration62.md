# Substituição nacional - round 62

## Objetivo fechado nesta rodada
Fechar os três blocos executáveis que faltavam após o command side:

- probes reais por tribunal
- executor industrial de migração por lote
- sync operacional de comunicações nacionais com correlação, deduplicação e reprocessamento

## Endurecimento adicional
- `tb_pjb_substituicao_execucao` agora também possui coluna `ver` para versionamento otimista
- `PjbSubstituicaoNacionalExecucaoRepository` ganhou `findLockedById(...)` com `PESSIMISTIC_WRITE`
- `PjbSubstituicaoNacionalCommandApplicationService` e `PjbSubstituicaoNacionalExecutionOrchestrator` passaram a usar leitura travada da execução nacional nas rotas críticas

## Estruturas novas

### Homologação por tribunal
- `PjbSubstituicaoTribunalHomologacaoProbeService`
- `PjbSubstituicaoTribunalHomologacaoProbeEntity`
- `PjbSubstituicaoTribunalHomologacaoProbeRepository`
- `PjbSubstituicaoHomologacaoProbeSituacao`

Tabela:
- `tb_pjb_subst_homologacao_probe`

O que persiste:
- probe por execução
- conector e ambiente
- score do gate
- evidências json
- resultado json
- situação final do probe

### Migração industrial por lote
- `PjbSubstituicaoMigracaoIndustrialBatchService`
- `PjbSubstituicaoMigracaoLoteEntity`
- `PjbSubstituicaoMigracaoLoteRepository`
- `PjbSubstituicaoMigracaoLoteSituacao`

Tabela:
- `tb_pjb_subst_migracao_lote`

O que persiste:
- lote por execução
- faixa de referência
- quantidade de itens
- checksum esperado/apurado
- divergências
- snapshot do lote
- situação do lote

### Sync operacional de comunicações nacionais
- `PjbSubstituicaoComunicacaoNacionalSyncService`
- `PjbSubstituicaoComunicacaoSyncCursorEntity`
- `PjbSubstituicaoComunicacaoSyncItemEntity`
- `PjbSubstituicaoComunicacaoSyncCursorRepository`
- `PjbSubstituicaoComunicacaoSyncItemRepository`
- `PjbSubstituicaoComunicacaoSyncSituacao`

Tabelas:
- `tb_pjb_subst_com_sync_cursor`
- `tb_pjb_subst_com_sync_item`

O que persiste:
- cursor por canal/janela
- namespaces de correlação e dedupe
- total recebido/deduplicado/correlacionado/reprocessável
- item individual com `dedupe_hash`
- chave de correlação por processo
- payload e resultado do item

## Orquestrador nacional
`PjbSubstituicaoNacionalExecutionOrchestrator` agora passa a materializar:

- probes de homologação no fluxo `HOMOLOGAR_TRIBUNAL`
- lotes industriais no fluxo `INICIAR_MIGRACAO_SOMBRA`
- cursores e itens de sync no fluxo `SINCRONIZAR_COMUNICACOES_NACIONAIS`

Os resultados persistidos entram também no `resultado_json` da execução nacional.

## Migrations
Nova migration:
- `V220__pjb_substituicao_nacional_execution_hardening.sql`

## Testes adicionados
- `PjbSubstituicaoMigracaoIndustrialBatchServiceTest`
- `PjbSubstituicaoTribunalHomologacaoProbeServiceTest`

## Observação honesta
A compilação global Maven não foi executada neste ambiente porque o wrapper do projeto depende de download externo da distribuição Maven.
