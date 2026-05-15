# tb_processo — Table Partitioning Plan

## Objective

Partition `tb_processo` by `data_distribuicao` (year) to:

- Limit sequential scan scope to relevant partitions
- Enable fast partition pruning on date-range queries
- Allow old partitions to be moved to cheaper storage tiers
- Reduce vacuum/analyze cost per partition

## Strategy — Declarative Range Partitioning (PG 11+)

```sql
-- Phase 1: Create partitioned parent (zero-downtime via pg_partman or manual swap)
CREATE TABLE tb_processo_partitioned (
    LIKE tb_processo INCLUDING ALL
) PARTITION BY RANGE (data_distribuicao);

-- Annual partitions — example for active years
CREATE TABLE tb_processo_y2020 PARTITION OF tb_processo_partitioned
    FOR VALUES FROM ('2020-01-01') TO ('2021-01-01');

CREATE TABLE tb_processo_y2021 PARTITION OF tb_processo_partitioned
    FOR VALUES FROM ('2021-01-01') TO ('2022-01-01');

CREATE TABLE tb_processo_y2022 PARTITION OF tb_processo_partitioned
    FOR VALUES FROM ('2022-01-01') TO ('2023-01-01');

CREATE TABLE tb_processo_y2023 PARTITION OF tb_processo_partitioned
    FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');

CREATE TABLE tb_processo_y2024 PARTITION OF tb_processo_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE tb_processo_y2025 PARTITION OF tb_processo_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

-- Default partition for rows outside defined ranges (legacy data)
CREATE TABLE tb_processo_default PARTITION OF tb_processo_partitioned DEFAULT;
```

## Migration Protocol (Zero-Downtime)

1. **Build phase** — create `tb_processo_partitioned` alongside `tb_processo` (no service impact)
2. **Copy phase** — `INSERT INTO tb_processo_partitioned SELECT * FROM tb_processo` in batches
3. **Freeze phase** — short maintenance window: rename `tb_processo` → `tb_processo_legacy`,
   rename `tb_processo_partitioned` → `tb_processo`
4. **Validate phase** — verify row counts, constraint integrity, FK references
5. **Cleanup phase** — drop `tb_processo_legacy` after N-day observation window

This approach avoids `ALTER TABLE ... PARTITION BY` (destructive) and eliminates downtime.

## Index Strategy per Partition

Each partition inherits indexes from the parent. Indexes are local to each partition,
meaning a query filtered to 2024 only scans the 2024 partition index, not all of history.

Combine with existing indexes from V257 (pg_trgm) and V258 (COALESCE sort) — they are
created on the parent and propagated to each partition automatically.

## Partition Pruning Requirements

The query planner prunes partitions only when the `WHERE` clause references the partition key
with an equality or range condition:

```sql
-- Prunable — planner scans only tb_processo_y2024
WHERE data_distribuicao >= '2024-01-01' AND data_distribuicao < '2025-01-01'

-- Not prunable — full scan across all partitions
WHERE EXTRACT(YEAR FROM data_distribuicao) = 2024
```

JPQL must use date literals or bind parameters comparable to the partition key directly,
not functions that wrap the column.

## Automation — pg_partman

For automated future partition creation and retention policy enforcement:

```sql
SELECT partman.create_parent(
    p_parent_table => 'public.tb_processo',
    p_control      => 'data_distribuicao',
    p_interval     => '1 year',
    p_premake      => 2
);
```

`pg_partman` creates upcoming partitions ahead of time (`p_premake=2` = 2 years ahead) and
can archive/detach old partitions per a configurable retention policy.

## RLS Compatibility

Row-Level Security policies (ADR-0041) defined on `tb_processo` are inherited by all partitions.
No per-partition RLS reconfiguration is required after the swap.

## Phasing

| Phase | Flyway Version | Description |
|---|---|---|
| Indexes | V257, V258 | Text search + sort indexes (done) |
| Materialized Views | V260 (planned) | Pre-aggregated analytics |
| Partitioning | V265 (planned) | Declarative range partition swap |
| pg_partman setup | V270 (planned) | Automated future partitions |

Partitioning is a significant DBA operation. Execute only after performance baseline is
established with V257/V258 indexes and validated against production load patterns.
