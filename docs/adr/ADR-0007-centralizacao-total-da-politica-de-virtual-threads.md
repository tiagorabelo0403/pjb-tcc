# ADR-0007 — centralização total da política de virtual threads

## Status
Aceito

## Contexto
A base já possuía a espinha `PjbVirtualThreadSpine` para impedir criação difusa de virtual threads no domínio. Ainda assim, algumas configurações Spring continuavam chamando `virtualThreads(true)` diretamente, o que mantinha a política de concorrência parcialmente espalhada.

## Decisão
Toda ativação de virtual threads deve ficar concentrada em `PjbVirtualThreadSpine`.

A espinha passa a concentrar:
- criação de executor por tarefa
- inicialização direta de threads virtuais nomeadas
- montagem de `SimpleAsyncTaskExecutor` com `virtualThreads(true)`

As configurações de runtime e conectores não ativam mais virtual threads diretamente. Elas apenas delegam à espinha central.

## Consequências
- a política de concorrência fica auditável em um único ponto
- futuras mudanças de estratégia ficam concentradas
- os testes de API surface passam a vigiar também `virtualThreads(true)`
- a sanidade do codebase passa a considerar `virtualThreads(true)` fora da espinha como violação
