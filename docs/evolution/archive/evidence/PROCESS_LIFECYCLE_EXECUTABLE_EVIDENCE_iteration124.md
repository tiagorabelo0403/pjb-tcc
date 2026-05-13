# Round 124 — process lifecycle com evidência executável da continuidade processual

## Objetivo
Fechar parte do delta de evidência executável no eixo de `process lifecycle`, ligando transições do `ProcessoLifecycleMachine` à materialização real da malha `casefile` em Postgres/Testcontainers.

## O que entrou
- `ProcessoLifecycleCaseContinuityFlowIT`
  - prova a abertura do ramo executivo a partir de processo transitado em julgado
  - prova a persistência do `CaseProceeding` raiz e do `CaseProceeding` de cumprimento
  - prova a aresta `EXECUTION_CONTINUATION`
  - prova o fechamento da malha no arquivamento com `CaseProceedingStatus.CLOSED`
  - prova a reativação no desarquivamento com retorno do root para `CaseProceedingRole.ROOT`
  - prova que rito penal não abre ramificação executiva incompatível
- budgets transacionais explícitos em writes centrais da continuidade:
  - `case-continuity.ensure-root.persist`
  - `case-continuity.lifecycle-sync.persist`
  - `case-continuity.unify-linked-cases.persist`

## Ganho técnico real
Antes desta rodada, havia boa cobertura unitária de regras do `ProcessoLifecycleMachine` e boa cobertura unitária do `CaseContinuityOrchestratorService`, mas faltava uma prova integrada de que a transição do lifecycle realmente atravessava a fronteira de persistência e deixava o grafo `casefile` coerente.

Com o round 124, o runtime passa a ter evidência concreta de que:
- transições válidas do lifecycle atualizam a malha de continuidade
- arquivamento e desarquivamento refletem no estado persistido dos proceedings
- o bloqueio jurídico de cumprimento incompatível não gera ramificação espúria no grafo

## Limitação honesta
O ambiente continua sem validação Maven ponta a ponta porque o Maven Wrapper não consegue baixar o binário externo do Maven. A rodada foi validada por inspeção estrutural, testes adicionados no código, guards Python e checagens locais de integridade do diff.

## Próximo alvo recomendado
- Testcontainers no eixo de ajuizamento com prova do command path principal
- Pact provider verification para detalhe/timeline pública
- continuidade da mitigação de N+1 nos painéis institucionais e leituras quentes
