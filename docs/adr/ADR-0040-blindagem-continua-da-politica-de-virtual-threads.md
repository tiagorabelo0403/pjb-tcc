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

## Atualização — regra por bytecode

A verificação deixou de ler o texto dos arquivos de produção. `PjbArchitectureTest.virtual_threads_apenas_no_spine`
reprova qualquer classe fora de `PjbVirtualThreadSpine` que chame ou referencie uma API que cria virtual thread
(`Thread.ofVirtual`, `Thread.startVirtualThread`, `Executors.newThreadPerTaskExecutor`,
`Executors.newVirtualThreadPerTaskExecutor`, os `virtualThreads`/`setVirtualThreads` dos executores e agendadores
assíncronos do Spring e os construtores de `VirtualThreadTaskExecutor` do Spring e de `VirtualThreadExecutor` do
Tomcat), inclusive quando a chamada passa por uma subclasse desses tipos, e `o_spine_e_quem_cria_virtual_threads`
prova que essa lista reconhece as criações do próprio spine. Por ser sobre bytecode, a regra não confunde menção em
string ou comentário com uso, e pega referência de método e de construtor. Criação por reflexão fica fora do alcance
de qualquer regra estática.
