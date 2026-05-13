# ADR-0008 — refatoração do codebase sanity e assembler processual

## Contexto

A leitura de sanidade do codebase e a fachada processual de completude estavam corretas funcionalmente, mas ainda acumulavam responsabilidades demais em poucas classes. Isso elevava custo de manutenção, aumentava risco de regressão em ajustes pequenos e deixava o mapeamento processual misturado com orquestração de serviços.

## Decisão

Foi adotada a mesma disciplina aplicada ao aprendizado estrutural:

- `PjbCodebaseSanityApplicationService` passa a ser apenas orquestrador com cache curto e refresh explícito
- a varredura do código, indexação de tipos, auditoria de imports, virtual threads, legado `judge` e tooling Maven foi deslocada para classes dedicadas
- o mapeamento processual de completude foi consolidado em `ProcessoCompletudeArquiteturalResponseAssembler`
- o endpoint processual de sanidade do código passa a aceitar `refresh=true`

## Consequências

### Positivas

- menor acoplamento na trilha de sanidade do codebase
- menor custo por request repetido graças ao cache curto
- fachada processual mais curta e focada em orquestração
- montagem de respostas centralizada e mais fácil de evoluir

### Trade-offs

- aumento do número de classes auxiliares na área de qualidade
- necessidade de manter testes de governança cobrindo cache, refresh e política centralizada
