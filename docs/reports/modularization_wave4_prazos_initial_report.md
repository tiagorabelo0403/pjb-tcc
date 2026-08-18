# Relatorio inicial da Onda 4 de modularizacao

## 1. Estado inicial do git

- Branch: `master`.
- Working tree antes da Onda 4: limpo.
- Commits locais ainda nao enviados antes desta onda:
  - `f5bf6cb arch(modular): bloquear crescimento de violacoes por baseline`
  - `8e53215 arch(modular): isolar modulo acordo do legado com ports e facades oficiais`
- `HEAD..origin/master`: vazio.
- Push: nao realizado.

## 2. Recorte escolhido

O recorte escolhido foi `prazos/notificacoes`, com foco apenas em prazos. Auditoria global ficou fora por decisao da rodada.

## 3. Por que prazos antes de ledger

Prazos ja possui nucleo funcional em `core/prazos`, `platform/jusos` e `service/processual/prazo`. Criar uma fronteira modular em volta desse nucleo reduz acoplamento sem reimplementar regra sensivel de contagem processual.

## 4. O que ja existia

- `PrazoProcessualNacionalService`.
- `NationalPrazoEngine`.
- `CalendarioForenseTribunalService`.
- Controller legado `/api/v1/processual/prazos`.
- Testes legados de prazo e arquitetura.

## 5. Problema arquitetural

Novos modulos poderiam chamar services e DTOs legados de prazo diretamente. Isso espalharia enums legados, regras de calendario e detalhes de superficie HTTP para dentro dos novos bounded contexts.

## 6. Plano da onda

- Criar `modules.prazos`.
- Publicar port e records internos sem entity JPA.
- Criar policy pura de dominio para entrada de calculo.
- Criar application service somente leitura.
- Criar adapter para `PrazoProcessualNacionalService`.
- Criar testes de dominio, application, adapter e arquitetura.
- Nao alterar endpoints, migrations ou services legados.

## 7. Riscos

- Prazos e area critica; reimplementar regra seria arriscado.
- O contrato inicial usa strings para nao vazar enums legados, exigindo validacao forte.
- Notificacoes ainda precisam de fronteira propria.

## 8. O que nao sera mexido agora

- Auditoria global.
- Ledger.
- Migration.
- Controller legado de prazos.
- Motor nacional de prazo.
- Calendario forense.
- Eliminacao global de `findAll`.
