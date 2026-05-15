# Flyway 10.x — PostgreSQL 18 Compatibility

## Status

Flyway 10.x (managed by Spring Boot BOM) is fully compatible with PostgreSQL 18.
No manual version pinning or workarounds are required.

## Validation Matrix

| Component | Version | Compatibility |
|---|---|---|
| Spring Boot | 3.5.x | Manages Flyway BOM |
| Flyway | 10.x (BOM) | Full PG 18 support |
| PostgreSQL | 18 | Supported from Flyway 10.4+ |
| JDBC Driver | 42.7.x | PG 18 wire-protocol compatible |

## Migration Strategy

All migrations in `src/main/resources/db/migration/` follow:

- **Naming**: `V{version}__{description}.sql` — sequential, no gaps
- **Idempotency**: DDL uses `IF NOT EXISTS` / `IF EXISTS` guards universally
- **Concurrency**: Index creation uses `CONCURRENTLY` to avoid table locks on live data
- **Non-destructive**: No `DROP TABLE`, `DROP COLUMN`, or `TRUNCATE` without explicit justification
- **RLS-aware**: Row-Level Security policies are applied per-operation (ADR-0041)

## PostgreSQL 18 Specific Notes

PostgreSQL 18 introduces changes to the query planner, WAL internals, and JSON path. None of
these affect Flyway execution, which operates over DDL and DML exclusively.

Key PG 18 features exploited by existing migrations:

- `GENERATED ALWAYS AS IDENTITY` (V1+)
- `USING GIN` with `pg_trgm` extension for ILIKE acceleration (V219, V257)
- Expression indexes for `COALESCE` ORDER BY clauses (V258)
- Partial indexes for selective queries (V219+)

## Upgrade Path

When upgrading Flyway beyond 10.x, verify:

1. `spring.flyway.baseline-on-migrate` remains `false` (production)
2. Checksum validation does not break existing migrations
3. `flyway.out-of-order=false` is enforced (default)

No migration rewrites are needed to support PG 18.
