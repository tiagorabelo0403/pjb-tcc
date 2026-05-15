# Aggregation Strategy — Analytics Cache Architecture

## Problem

Analytical aggregation queries on `tb_processo` (e.g., `agregadosPorRamo`, `agregadosPorRamoETribunal`)
are expensive: full or partial sequential scans with GROUP BY, COUNT, AVG across potentially millions
of rows. Calling these directly in request-path services produces unacceptable latency under load.

Additionally, Spring AOP cannot intercept `@Cacheable` on `private` methods — proxy interception
only applies to public methods of Spring-managed beans called through the proxy reference.

## Solution

### Layer 1 — `ProcessoAnalyticsAggregationService`

A dedicated `@Service` bean with `public @Cacheable` methods. This is the only class permitted
to call `ProcessoRepository#agregadosPorRamo` and `ProcessoRepository#agregadosPorRamoETribunal`.

```
ProcessoBuscaAnalyticsApplicationService  ──┐
                                             ├──► ProcessoAnalyticsAggregationService ──► ProcessoRepository
JurimetriaService  ─────────────────────────┘
```

Cache names and their TTL/size bounds (configured via `application.yml` / environment variables):

| Cache | Key | Default TTL | Default Max |
|---|---|---|---|
| `processo_analytics_ramo` | `ramo` | 10 min | 500 |
| `processo_analytics_tribunal` | `ramo:tribunal` | 10 min | 1000 |
| `jurimetria_relatorio` | — | 15 min | 200 |

All caches are backed by `PjbBoundedLocalCacheManager` (Caffeine, bounded size, timed expiry).

### Layer 2 — Materialized View (planned, PG 18)

For dashboards and exports requiring sub-second response on full-corpus aggregations, a
PostgreSQL Materialized View refreshed on a schedule provides pre-computed results:

```sql
CREATE MATERIALIZED VIEW mv_processo_analytics_ramo AS
SELECT
    ramo_direito                       AS ramo,
    COUNT(*)                           AS total,
    COUNT(*) FILTER (WHERE status IN ('JULGADO', 'ARQUIVADO'))  AS julgados,
    COUNT(*) FILTER (WHERE recurso_pendente = true)             AS recursais,
    COUNT(*) FILTER (WHERE status NOT IN ('JULGADO', 'ARQUIVADO', 'EXTINTO')) AS ativos,
    COALESCE(AVG(
        EXTRACT(EPOCH FROM (
            COALESCE(data_julgamento, CURRENT_TIMESTAMP) - data_distribuicao
        )) / 86400.0
    ), 0)                              AS tempo_medio_dias
FROM tb_processo
WHERE ramo_direito IS NOT NULL
GROUP BY ramo_direito
WITH DATA;

CREATE UNIQUE INDEX ON mv_processo_analytics_ramo (ramo);
```

Refresh via scheduled job (not `@Scheduled` — see ADR-0051 for async governance):

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_processo_analytics_ramo;
```

## Invariants

- `ProcessoRepository` aggregation methods are NOT called directly from services other than
  `ProcessoAnalyticsAggregationService`.
- Cache invalidation is time-based (TTL). Explicit eviction is not required for analytics
  workloads where eventual consistency within the TTL window is acceptable.
- Cache sizes are bounded to prevent unbounded heap growth under adversarial query patterns.
