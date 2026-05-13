# ADR-0040 — blindagem contínua da política de virtual threads

## Status
Aceito

## Contexto

O PJB já havia consolidado a espinha `PjbVirtualThreadSpine` como ponto único para ativação de virtual threads. Mesmo assim, sem uma verificação automática, ainda existia o risco de regressão por criação direta de virtual threads ou ativação dispersa de `.virtualThreads(true)` fora da espinha.

## Decisão

A base passou a contar com o teste estrutural `PjbVirtualThreadPolicyTest`, que percorre os arquivos de produção e reprova o build quando encontra padrões de ativação de virtual threads fora de `PjbVirtualThreadSpine`.

Os padrões protegidos são:

- `Thread.ofVirtual().start(...)`
- `Executors.newThreadPerTaskExecutor(Thread.ofVirtual(...))`
- `.virtualThreads(true)`

## Consequências

### Positivas

- a disciplina de concorrência virtual deixa de depender de convenção manual
- a espinha central passa a ser protegida por build
- futuras evoluções de concorrência continuam auditáveis em um único ponto

### Negativas

- qualquer evolução legítima da política de virtual threads precisará primeiro atualizar a disciplina estrutural correspondente
