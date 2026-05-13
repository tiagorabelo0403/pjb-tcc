# Round 127 — Ajuizamento command phase split

## Objetivo

Reduzir retenção transacional e acoplamento operacional no `AjuizarProcessoCommand`, tirando do path central o que não precisa competir por conexão/lock durante a persistência do ajuizamento.

## O que entrou

- `AjuizarProcessoCommand` ficou restrito ao command path de validação, canonicalização, snapshot mínimo, persistência e publicação do evento.
- `ProcessoAjuizadoEvent` passou a carregar `juizo100Digital` para permitir reexecução governada dos efeitos pós-commit sem depender do boundary HTTP.
- Novo listener `AjuizarProcessoCommandPostCommitEffectsService` com `@TransactionalEventListener(AFTER_COMMIT)`.
- `JudicialConnectorLifecycleService.submitAndSynchronize(...)` passou a ter proteção explícita com `@CircuitBreaker`, `@Retry` e `@Bulkhead`.
- Budget explícito: `ajuizamento.command.post-commit.persist`.
- Migração de efeitos não bloqueantes para o pós-commit:
  - protocolo judicial real e sincronização de estado externo
  - auditoria imutável do ajuizamento
  - consolidação de resumo por IA com persistência curta
- Novos testes:
  - `AjuizarProcessoCommandPostCommitEffectsServiceTest`
  - ampliação de `PjbAjuizamentoCommandPathArchitectureTest`

## Ganho arquitetural

O `AjuizarProcessoCommand` deixou de segurar conectores externos, IA e auditoria pesada dentro da mesma fronteira transacional usada para persistir o ajuizamento. Isso reduz pressão sobre pool/conexão, diminui risco de transação longa e deixa o command path mais auditável.

## Limitação honesta

A rodada foi validada por inspeção estrutural, guards Python, `git diff --check` e commit local temporário. O build Maven completo continua não comprovado neste ambiente por bloqueio externo do Maven Wrapper.
