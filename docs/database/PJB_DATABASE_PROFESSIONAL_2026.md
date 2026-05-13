# PJB Database Professional Blueprint 2026

## Objetivo
Elevar a malha relacional do PJB para um padrão profissional com PostgreSQL 18, read mesh, edge TCP, PgBouncer, observabilidade nativa, retenção governada e particionamento administrável.

## Postura adotada
- primary horizontal como trilha principal de escrita
- read mesh horizontal com réplica estrita e edge TCP para entrada estável
- elasticidade vertical por pool, disco e memória das instâncias
- cache quente em Redis e cache local em Caffeine para aliviar leitura repetida
- filas Kafka/outbox/read-model para desafogar escrita síncrona
- shield de pressão de banco na borda da API

## Ferramentas acopladas
- PostgreSQL 18
- pg_stat_statements
- pgcrypto
- PgBouncer
- HAProxy TCP edge para rw/ro
- read replica
- Hikari com keepalive, leak threshold, MBeans e session application name

## Eixos de endurecimento
- particionar tabelas quentes por janela temporal administrada
- retenção governada por tabela
- observabilidade SQL e I/O
- conexão por edge estável em alta disponibilidade
- transaction pooling no PgBouncer
- sessão com application name, keepalive e batched inserts
- readiness estrutural para RLS e legal hold
