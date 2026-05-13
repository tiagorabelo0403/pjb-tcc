# Round 97 — compile recovery do cluster jurisdicao/websocket

## Escopo
- `model/entity/Jurisdicao.java`
- `model/entity/JurisdictionEngine.java`
- `tracker/UserActivitySocketHandler.java`
- `adapter/strategies/config/WebSocketConfig.java`

## Mudanças
- remoção de Lombok do cluster;
- construtores explícitos para websocket/config;
- accessors explícitos para `Jurisdicao`;
- builders explícitos para `JurisdictionEngine`;
- testes e guard arquitetural do lote recuperado.

## Validação honesta
- compilação dirigida com `javac` e stubs mínimos passou;
- `runtime_concurrency_guard.py` passou;
- sem afirmação de build Maven global verde;
- sem afirmação de compile total do `pjb-api`;
- sem afirmação de Docker estável.
