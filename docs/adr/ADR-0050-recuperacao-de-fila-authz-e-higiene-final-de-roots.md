# ADR-0050 — recuperação de fila AUTHZ e higiene final de roots

## Status
Aceito

## Contexto

A camada analítica AUTHZ já possuía fila persistente, retry, deduplicação por bucket e backfill administrativo. Ainda assim, permaneceram dois riscos operacionais reais:

- itens poderiam ficar presos em `PROCESSING` após falha de worker, encerramento abrupto da instância ou timeout operacional;
- o projeto continha artefatos `.class` dentro de `src/main/java`, o que viola o layout canônico do Maven, polui o root do código-fonte e fragiliza a abertura limpa em IDE/build.

Além disso, a fila carecia de uma rotina explícita de limpeza dos itens concluídos, o que manteria crescimento infinito da tabela operacional mesmo após materializações bem-sucedidas.

## Decisão

Foi adotado o seguinte fechamento:

1. a fila analítica AUTHZ passa a suportar recuperação de itens `PROCESSING` considerados estagnados com base em `processing-timeout-ms`;
2. entrou limpeza periódica dos itens `COMPLETED` com base em `completion-retention-days`;
3. a superfície administrativa ganhou operações explícitas de recuperação e limpeza da fila;
4. o `application.yml` passou a declarar os knobs operacionais da fila AUTHZ;
5. os artefatos `.class` foram removidos de `src/main/java`, o `.gitignore` passou a bloquear esse tipo de vazamento e o módulo IntelliJ foi endurecido com JDK 21 explícito.

## Consequências

### Positivas

- a fila analítica AUTHZ fica mais resiliente a falhas de worker e reinícios abruptos;
- a operação passa a enxergar contagem de itens estagnados e elegíveis para limpeza;
- o root do projeto volta a respeitar o layout canônico do Maven;
- a abertura em IDE fica menos sujeita a drift de JDK.

### Negativas

- a malha de refresh ganha mais alguns knobs operacionais para governança;
- a recuperação de `PROCESSING` estagnado aumenta o número de reprocessamentos em cenários de falha severa, embora preserve correção dos buckets.
