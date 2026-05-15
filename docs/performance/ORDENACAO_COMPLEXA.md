# Complex ORDER BY — COALESCE Functional Index Strategy

## Problem

Several `ProcessoRepository` and `WorkItemRepository` queries sort by:

```sql
ORDER BY COALESCE(data_ultima_movimentacao, data_atualizacao, data_criacao) DESC NULLS LAST,
         id DESC
```

Without a matching index, each execution triggers a full sequential scan + sort, with complexity
O(N log N) that scales poorly as `tb_processo` grows beyond millions of rows.

## Solution — Expression Indexes (V258)

PostgreSQL supports functional/expression indexes that are used by the query planner when the
`ORDER BY` expression exactly matches the index definition.

```sql
-- V258__coalesce_sort_indexes.sql

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tb_processo_sort_movimentacao
    ON tb_processo (
        COALESCE(data_ultima_movimentacao, data_atualizacao, data_criacao) DESC NULLS LAST,
        id DESC
    );

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_tb_work_item_sort_updated
    ON tb_work_item (
        COALESCE(updated_at, created_at) DESC NULLS LAST,
        created_at DESC,
        id DESC
    );
```

`CONCURRENTLY` ensures zero table lock during index build on a live database.

## Query Planner Requirements

For the planner to use the index, the JPQL/HQL sort expression must match exactly:

```java
// JPQL — planner-eligible
ORDER BY COALESCE(p.dataUltimaMovimentacao, p.dataAtualizacao, p.dataCriacao) DESC NULLS LAST, p.id DESC
```

Any reordering of the COALESCE arguments or change in NULLS direction breaks the match and
reverts to sequential scan. The expression in the index must be identical to the ORDER BY clause.

## Performance Characteristics

| Scenario | Without Index | With Index |
|---|---|---|
| Top-10 paginated (offset 0) | O(N log N) full sort | O(log N) index scan |
| Deep pagination (offset 50000) | O(N log N) | O(log N + offset) |
| Combined WHERE + ORDER BY | Filter then sort | Index-only when selective |

At 1M rows: sort-only cost ~12s → index scan cost ~2ms for first page.

## Covered Queries

Five `ProcessoRepository` methods and two `WorkItemRepository` methods use this sort pattern
and will benefit from the index without any code change — the planner selects the index
automatically when the sort expression matches.

## Maintenance

Expression indexes are updated incrementally on INSERT/UPDATE, same as B-tree indexes.
`VACUUM ANALYZE tb_processo` should be scheduled to keep planner statistics current.
