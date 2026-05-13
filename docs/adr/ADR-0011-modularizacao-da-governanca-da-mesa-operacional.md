# ADR-0011 — modularização da governança da mesa operacional institucional

## Status
Aceito

## Contexto

`InstitutionalOperationalDeskGovernanceApplicationService` concentrava resolução de perfil, inferência territorial, heurística de workflow, topologia de unidade, filas, fronteiras de atribuição, contrapartes, atos operacionais e montagem final do agregado. A classe havia ultrapassado mil linhas, misturando leitura de sinais, derivação de snapshot e composição do resultado.

Esse formato aumentava o custo de manutenção da trilha `core/comunicacao/institucional/panel`, dificultava validação localizada e deixava a lógica muito sensível a alterações futuras em fila, unidade, vara, gabinete, UPJ e segundo grau.

## Decisão

A governança da mesa operacional passa a seguir uma divisão explícita:

- `InstitutionalOperationalDeskGovernanceApplicationService` fica como orquestrador curto
- `InstitutionalOperationalDeskSnapshotResolver` concentra a leitura de perfil, unidade, escopo, sinais e flags de workflow
- `InstitutionalOperationalDeskSupport` concentra normalização, detecção de tokens, fingerprint territorial, eixo judicial, tipo de unidade e resolução de capacidades/perfis
- `InstitutionalOperationalDeskGovernanceAssembler` concentra filas, fronteiras, contrapartes, atos, augmentations e montagem final do agregado
- `InstitutionalOperationalDeskSnapshot` e `InstitutionalOperationalDeskUnitFingerprint` viram contratos internos explícitos da trilha

## Consequências

### Positivas

- a classe principal deixa de concentrar inferência e montagem completa
- a leitura do fluxo operacional fica separada entre snapshot, utilitários e composição final
- filas, fronteiras e augmentations continuam preservadas, mas agora em ponto próprio
- a evolução futura de `gabinete`, `upj`, `protocolo/distribuição`, `secretaria de segundo grau` e `cejusc` passa a ser menos arriscada
- fica mais simples endurecer testes por responsabilidade sem acoplar tudo a um único arquivo

### Custos

- surgem mais contratos internos no pacote
- a trilha de assembler ainda permanece densa e poderá ser quebrada novamente por subtópico em rodada futura

## Direção futura

A próxima quebra natural dessa trilha é separar o assembler por subeixos:

- filas e fronteiras
- atos operacionais por papel
- augmentations por tipo de unidade
- augmentations por eixo judicial
