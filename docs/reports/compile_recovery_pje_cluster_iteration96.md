# Round 96 — compile recovery do cluster PJe legado

Nesta rodada o foco saiu do eixo recursal e atacou um cluster antigo do `pjb-api` que ainda adicionava ruído real na recuperação de compile.

## O que entrou

- `RamoDireito` sem Lombok, com getters explícitos para `codigo`, `descricao` e `categoria`;
- `PJeAutenticacaoResponse`, `PJeSubmissaoResponse` e `PJeAndamentoResponse` migrados para `record`, com cópia defensiva de mapas/listas;
- `PJeSubmissionWorker` com construtor explícito, sem `@RequiredArgsConstructor`;
- testes unitários e teste arquitetural do cluster recuperado.

## Validação honesta

- compilação dirigida com `javac` do enum e dos DTOs passou;
- compilação dirigida com stubs mínimos do worker e do cluster PJe passou;
- runner local do cluster PJe passou;
- `runtime_concurrency_guard.py` passou;
- não foi afirmado build Maven global verde;
- não foi afirmado compile total do `pjb-api`;
- não foi afirmado Docker estável.

## O que ainda falta

- continuar fechando os hotspots de compile fora do recursal com mais lotes pequenos e seguros;
- decidir o próximo lote entre entidades antigas pesadas (`Jurisdicao`, `Processo`, `Usuario`, `Equipe`) e adaptadores/tracker antigos com dependências externas.
