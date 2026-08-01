# Nota Técnica — PostgreSQL 17 como Baseline Estável do PJB

## Status atual

**PostgreSQL 17** é o baseline estável e homologado do PJB.

PostgreSQL 18 não é beta em 2026, mas **não está homologado no PJB** até
que os critérios abaixo sejam atendidos. PostgreSQL 17 permanece como
versão de referência até validação formal.

## Matriz de compatibilidade atual

| Componente                    | Versão                    | Status              |
|-------------------------------|---------------------------|---------------------|
| Spring Boot                   | 3.5.x                     | BOM ativo           |
| Flyway                        | 10.x (BOM)                | Compatível com PG17 |
| PostgreSQL                    | **17** (docker)           | Homologado          |
| JDBC Driver                   | 42.7.x                    | Compatível PG17     |
| docker-compose (primário)     | `pgvector/pgvector:pg17`  | Imagem fixada — Postgres 17 real com extensão `vector` pré-compilada (necessária pela migration V307 e pelo `VectorSearchServicePgVector`) |
| docker-compose (read-replica) | `pgvector/pgvector:pg17`  | Deve casar exatamente com o primário — replicação física exige mesmas extensões |
| Testcontainers                | `pgvector/pgvector:pg17`  | Registrado como `asCompatibleSubstituteFor("postgres")` em `PjbIntegrationTestBase` |

## Por que não PG18 ainda

1. **Sem homologação formal**: PG18 não passou por ciclo de testes de
   regressão no PJB — migrations Flyway, RLS policies e extensões (pg_trgm,
   unaccent, pgcrypto) precisam ser validadas.
2. **Mudanças de wire protocol**: PG18 pode introduzir mudanças de protocolo
   que requerem verificação do driver JDBC (42.7.x).
3. **Testcontainers**: imagem `postgres:18` ainda não confirmada nos ambientes
   de CI utilizados pelo projeto.

## Critérios para migrar para PG18

- [ ] Suite completa de testes de integração aprovada com `postgres:18`
- [ ] Guards de governança sem regressão
- [ ] JDBC driver 42.8+ ou confirmação de compatibilidade
- [ ] Aprovação da equipe de infraestrutura

## Histórico

- `docker-compose.yml` e `docker-compose.read-replica.yml` alterados de
  `postgres:18` para `postgres:17` durante auditoria de governança (2026-05).
- Tag de imagem mais reprodutível (`postgres:17`) adotada em vez de
  `postgres:latest` para garantir determinismo entre ambientes.
- Imagem trocada de `postgres:17` para `pgvector/pgvector:pg17` no primário,
  read-replica e Testcontainers (2026-08). A imagem `pgvector/pgvector:pg17`
  **é** o `postgres:17` oficial com a extensão `vector` pré-compilada — drop-in
  para todos os efeitos de wire protocol, replicação e schema. Necessária pela
  migration `V307__ai_vector_store_pgvector.sql`; sem ela, o `CREATE EXTENSION vector`
  falha e o Flyway não sobe. Read-replica **deve** casar a imagem do primário
  porque replicação física do PostgreSQL exige que o standby carregue as mesmas
  extensões — se o primário ativa `vector` e o standby estiver em `postgres:17`
  puro, o standby falha ao replicar catálogos de sistema.
