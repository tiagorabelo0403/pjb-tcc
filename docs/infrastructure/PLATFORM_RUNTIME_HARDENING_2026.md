# Platform Runtime Hardening 2026

## Eixos fechados nesta rodada

- governança soberana de execução assíncrona para serviços de negócio sensíveis
- redução de fronteiras duplas de async em ingestão documental
- remoção de `synchronized` em lifecycles críticos de jobs e listen/notify
- rastreio automatizado de anti-patterns de concorrência na base

## Diretrizes operacionais

### Thread pool saturado

Toda submissão assíncrona nova deve passar pelo `PjbExecutionOrchestrator`, com lane e timeout explícitos.

### Futures espalhados

Serviços de domínio e aplicação não devem abrir `CompletableFuture.supplyAsync/runAsync` diretamente.

### Connection pool do banco

A pressão do pool continua centralizada no `PjbRuntimePressureService`, no filtro de admissão e no escudo de pressão da API.

### Memory leak silencioso

A leitura de heap, metaspace, direct buffer e GC segue concentrada no `PjbRuntimePressureService`. A regra prática é não abrir retenção ad hoc em caches, buffers ou filas sem orçamento explícito.

### Race condition

Ciclos de vida críticos devem usar lock explícito e estado atômico. O objetivo é evitar dupla partida, dupla parada e corrida de interrupção.

### Sincronia e contenção

Métodos `synchronized` em serviços críticos devem ser migrados para lock explícito, estado atômico ou estruturas imutáveis, conforme o caso.
