# ADR-0045 — read model persistente da trilha AUTHZ

## Status
Aceito

## Contexto

A trilha AUTHZ já havia evoluído para uma decisão explicável, persistida no ledger e consultável em runtime por meio de um registry em memória. Apesar disso, ainda existia uma limitação estrutural relevante: a consulta administrativa dependia fortemente de estado quente do processo de aplicação.

Para governança operacional, auditoria forense, análise pós-incidente e uso em ambientes com múltiplas instâncias, a trilha precisava deixar de ser apenas runtime-bound e passar a ter um read model persistente, independente do ciclo de vida do processo Java.

## Decisão

Foi criado um read model persistente dedicado para a trilha AUTHZ:

- `PjbAuthorizationTrailReadModelEntry`
- `PjbAuthorizationTrailReadModelRepository`
- `PjbAuthorizationTrailReadModelService`

A materialização ocorre no mesmo ponto em que a decisão AUTHZ já é auditada e registrada em runtime, dentro de `PjbAuthorizationAuditFacade`.

A superfície administrativa de consulta passa a suportar seleção explícita da fonte:

- `PERSISTED`
- `RUNTIME`
- `MERGED`

Também foram adicionados filtros temporais, de governança, de step-up satisfeito e de tipo de ator.

## Consequências

### Positivas

- a trilha AUTHZ passa a sobreviver ao ciclo de vida do processo e à rotação de pods/instâncias
- a consulta administrativa deixa de depender apenas de memória local
- a operação pode comparar runtime e persistido sem reabrir a malha de autorização
- o sistema fica mais preparado para auditoria forense, exportação e futuros cubos analíticos

### Negativas

- aumenta a massa estrutural da capacidade AUTHZ com uma tabela, migration e repositório adicionais
- exige disciplina futura de retenção, particionamento e exportação fria caso o volume cresça de forma relevante
- consultas `MERGED` precisam tratar deduplicação por `payloadHash`, o que adiciona alguma complexidade ao assembler
