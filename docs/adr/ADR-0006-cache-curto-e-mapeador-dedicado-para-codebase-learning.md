# ADR-0006 — Cache curto e mapeador dedicado para codebase-learning

## Contexto

O relatório `codebase-learning` passou a oferecer hotspots, trilhas internas de extração, blueprints e fluxos críticos. A implementação anterior concentrava scanner de arquivos, análise estrutural, definição de fluxos e mapeamento de DTOs em poucas classes grandes, além de revarrer toda a base a cada chamada HTTP.

Isso elevava custo operacional desnecessário, aumentava o tamanho das classes e duplicava mapeamentos entre as superfícies administrativa e processual.

## Decisão

A solução canônica adotada nesta rodada foi:

- separar a construção do snapshot estrutural em componentes dedicados de layout, settings, leitura de fontes e builder
- manter a application service como orquestradora curta, responsável por cache curto e refresh explícito
- introduzir `PjbCodebaseLearningResponseMapper` como mapeador dedicado para evitar duplicação entre governance e processual
- expor `refresh=true` nos endpoints administrativo e processual para forçar revarredura quando necessário
- usar TTL curto em memória para reduzir pressão de disco/CPU em chamadas repetidas do relatório

## Consequências

### Ganhos

- classes menores e mais organizadas por responsabilidade
- menor custo por chamada na inspeção estrutural do projeto
- refresh explícito sem necessidade de reiniciar a aplicação
- menos duplicação de transformação entre aggregate e DTOs
- trilha mais segura para evoluir métricas sem inflar controllers ou facades

### Restrições assumidas

- o snapshot não é distribuído entre nós; ele é local ao processo
- o cache curto não substitui execução periódica de `verify`
- o refresh explícito deve ser usado quando a base mudar e o relatório precisar refletir a alteração imediatamente

## Status

Aceito.
