# Round 149 — refinamento da agenda operacional do oficial de justiça

## Objetivo
Reduzir a concentração de heurística territorial, telemetria live e composição de painel dentro da `OficialJusticaAgendaOperacionalService`, preservando a service principal como borda fina de orquestração.

## Entradas materiais desta rodada
- `OficialJusticaAgendaAssemblySupport`
- `OficialJusticaAgendaTelemetrySupport`
- `OficialJusticaAgendaPanelSupport`
- `OficialJusticaAgendaSupportUtils`
- `OficialJusticaAgendaTerritorialHint`
- `OficialJusticaAgendaLiveEventDigest`
- `OficialJusticaAgendaAssemblySupportTest`
- `OficialJusticaAgendaPanelSupportTest`
- `OficialJusticaAgendaOperacionalServiceRefinementArchitectureTest`

## O que saiu da service principal
- montagem de `StopRow`
- enrich de quick actions e formal model
- heurística de status/cor/esfera/cobertura
- hints territoriais e digests live
- leitura de checkpoint/encerramento com frustração estruturada
- reordenação dinâmica da rota
- buckets de status e legenda de cores
- resumo vivo de replanejamento
- mapa do painel resumo

## Como ficou separado
### AssemblySupport
- stop input para roteirização
- montagem de `StopRow`
- desk rooms
- filtros, scope, summary e alerts
- composição territorial do oficial

### TelemetrySupport
- hints territoriais
- seleção de endereço candidato
- digests live por work item
- frustração estruturada e estratégia de retorno

### PanelSupport
- reorder dinâmico por peso operacional e massa territorial
- buckets de status
- legenda de cores
- resumo vivo de replanejamento
- composição do mapa de `painelResumo()`

## Evidência executável adicionada
### `OficialJusticaAgendaAssemblySupportTest`
Trava:
- alerta federal na malha operacional
- frustração estruturada
- replanejamento recomendado
- quick actions com formal model/manual/automatic actions

### `OficialJusticaAgendaPanelSupportTest`
Trava:
- reorder dinâmico por atraso/replanejamento/território
- resumo de replanejamento com frustração e adiadas
- mapa de painel com scope/summary/topStops/statusBuckets

### `OficialJusticaAgendaOperacionalServiceRefinementArchitectureTest`
Trava:
- service com no máximo 7 dependências
- presença dos três suportes dedicados
- proibição de reabsorção dos métodos removidos

## Resultado estrutural
- `OficialJusticaAgendaOperacionalService`: 1198 -> 101 linhas

## Guardrails
Rodadas executadas:
- `architecture_hygiene_guard.py`
- `constructor_injection_guard.py`
- `runtime_concurrency_guard.py`
- `transactional_hotspot_guard.py --fail-on-missing-budgets`
- `config_taxonomy_guard.py`
