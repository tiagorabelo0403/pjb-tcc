# ADR-0061 — Política de Resiliência e Recuperação

**Status:** Aceito
**Data:** 2026-05-30

## Contexto

O PJB é infraestrutura crítica de justiça. Falhas de disponibilidade têm consequências jurídicas diretas: prazos expiram, comunicações processuais ficam pendentes, atos não são praticados. Este ADR formaliza RPO/RTO, circuit breakers implementados e a política de supply chain security.

## Decisão

### RPO/RTO por Componente

| Componente | RPO | RTO | Justificativa |
|------------|-----|-----|---------------|
| PostgreSQL (dados processuais) | 1 hora | 4 horas | Atos processuais têm efeito jurídico imediato |
| Kafka (se houver) | 0 (transacional) | 2 horas | Outbox garante entrega exatamente-uma-vez |
| Redis (se houver) | Perda aceitável | 30 minutos | Cache e sessões são recomputáveis |
| Elasticsearch (se houver) | Reindexável | 2 horas | Índices são projeções, não fonte de verdade |

### Circuit Breakers Implementados

- `QualifiedDocumentSignatureEnvelopeService` → timeout de assinatura com fallback auditado
- `JudicialConnectorRegistry` → fallback gracioso para conectores externos não disponíveis
- Redis → fallback `NoOp` quando cluster indisponível (`PJB_LIVE_CLUSTER_ENABLED=false` via docker-compose)

### Supply Chain Security

- SBOM CycloneDX 1.6 gerado automaticamente em cada ciclo `package`
- Dependency audit OWASP dependency-check: CVSS ≥ 9 bloqueia o build de CI
- `jackson-databind` fixado para evitar range transitivo via `Yubico webauthn-server-core`
- `maven-enforcer-plugin`: `banSnapshots=true`, `requireReleaseDeps`, `requirePluginVersions`
- `dependency:go-offline` removido do Dockerfile (resolvia `jackson-databind:2.19.5-SNAPSHOT`)

### Política de Atualização de Dependências

- Dependências com CVE CVSS ≥ 7 devem ser resolvidas em até 30 dias
- Dependências com CVE CVSS ≥ 9 bloqueiam CI imediatamente
- SBOM deve ser comparado entre releases para detectar adições não auditadas
- Qualquer dependência nova com escopo `compile`/`runtime` exige revisão de licença

## Pendências (exigem infraestrutura de staging)

- Backup restore test automatizado (`pg_dump`/`pg_restore`) em ambiente isolado
- Teste de queda de Redis/Kafka/Elasticsearch com fallback verificado por IT
- Outbox pattern com DLQ e alerta de mensagem morta por threshold
- Chaos testing controlado (falha de rede, lentidão de banco, exaustão de pool)
- PIT (Pitest) com threshold de mutação ≥ 60% no núcleo judicial

## Consequências

- Supply chain security ativa a partir deste commit (SBOM + enforcer + CI gate)
- RPO/RTO formalizados para guiar decisões de infraestrutura futuras
- Pendências documentadas como bloqueadores para ambiente de staging produtivo
