# Round 125 — Ajuizamento executable evidence e budgets do command path

## O que entrou

Esta rodada atacou o déficit de evidência executável do eixo de ajuizamento e fechou budgets explícitos no command path central.

Entrou de forma concreta:

- budgets transacionais explícitos em:
  - `AjuizamentoService.ajuizar(...)`
  - `AjuizarProcessoCommand.execute(...)`
  - `ProcessoPostAjuizamentoOrchestratorService.onProcessoAjuizado(...)`
- novo helper de consulta no repositório de outbox para rastrear eventos por agregado
- novo `AjuizamentoServiceFlowIT` com Postgres/Testcontainers cobrindo:
  - ajuizamento com emissão real de outbox
  - consolidação de snapshot inicial de distribuição
  - criação de work item de revisão inicial da secretaria quando existe pendência material
  - criação de conclusão inicial ao gabinete quando o ajuizamento já chega saneado
  - rollback honesto do fluxo quando o teto processual bloqueia o ajuizamento de juizado especial
- novo teste arquitetural `PjbAjuizamentoCommandPathArchitectureTest`
- nova provider verification Pact da surface de intenção de ajuizamento (`/api/v1/ai/ajuizamento/ramos`)

## O que esta rodada passou a provar

No plano executável, o bounded context de ajuizamento agora demonstra que:

- o serviço central de ajuizamento persiste o processo e materializa evento de outbox com agregado correto
- o pós-commit de ajuizamento cria fila operacional coerente com o estado material do caso
- a secretaria recebe revisão inicial quando há pendência relevante de qualificação do polo passivo
- o gabinete recebe conclusão inicial quando o ajuizamento já está saneado e distribuído
- a alçada econômica do juizado especial bloqueia o ajuizamento antes de persistir processo ou outbox

## Ganho estrutural

Este round reduz a distância entre arquitetura declarada e comportamento verificável no eixo de entrada do processo.

O ajuizamento deixou de depender apenas de leitura de código e passou a ter prova executável de:

- snapshot procedimental/distribuição
- efeito transacional mínimo esperado
- emissão de outbox
- derivação de fila institucional pós-commit
- rollback quando a regra jurídica de alçada impede o ingresso

## Limitação honesta

O `AjuizarProcessoCommand` ainda concentra trabalho secundário demais dentro da mesma fronteira transacional, especialmente auditoria/enriquecimento/IA/conector.

O budget explícito fecha a governança desta superfície, mas a próxima melhoria natural continua sendo separar com mais rigor:

- persistência mínima do command path
- side effects pós-commit
- integração externa e enriquecimento fora da retenção principal de conexão

## Próximo alvo recomendado

- phase split real do `AjuizarProcessoCommand` para reduzir retenção transacional do command path principal
- ampliar Testcontainers em `AjuizarProcessoCommand`/controller multipart
- expandir Pact provider verification para detalhe/timeline pública e outras superfícies de consulta
