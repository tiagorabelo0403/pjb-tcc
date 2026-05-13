# Round 119 — Processo sigiloso com envelope RLS e read model seguro

## O que entrou

- `ProcessoSigiloRlsEnvelope`
- `ProcessoSigiloRlsEnvelopeService`
- `PjbProcessoSigiloRlsFilter`
- `V221__processo_sigilo_secure_read_model.sql`
- `ProcessoSigiloRlsEnvelopeServiceTest`
- `PjbProcessoSigiloRlsFilterTest`
- `ProcessoSigiloSecureReadModelIT`

## O que isso resolve

Esta rodada sai do catálogo LGPD puramente declaratório e cria uma ponte executável entre:

- classificação LGPD da entidade `Processo`
- nível de sigilo processual
- materialização de escopo institucional mínimo
- read model SQL seguro para processos sigilosos

## Como a trilha funciona

1. A entidade `Processo` já está classificada no `DataClassificationCatalog`.
2. O `ProcessoSigiloRlsEnvelopeService` transforma isso em envelope operacional:
   - clearance mínimo exigido
   - tribunal requerido
   - unidade requerida
   - necessidade de step-up
   - necessidade de certificado qualificado
   - recomendação de modo somente leitura
3. O `PjbProcessoSigiloRlsFilter` injeta esses hints nos fluxos sigilosos:
   - `/api/v1/processual/unificado/{processoId}/sigilo-inteligente`
   - `/api/v1/processual/unificado/{processoId}/sigilo-notificacoes`
   - `/api/v1/processos/sigilo/zk/{processoId}/challenge`
4. A migration `V221` cria `vw_pjb_processo_sigilo_secure`, que lê `current_setting(app.pjb_*)` para aplicar escopo de clearance, tribunal e unidade.

## Por que isso é melhor que blueprint puro

Antes existia blueprint/documento de RLS.
Agora existe:

- materialização aplicacional do escopo sigiloso
- filtro HTTP que expõe os hints necessários ao data plane
- read model SQL executável
- teste real com PostgreSQL via Testcontainers

## Limitação honesta

Ainda não há interceptor de conexão aplicando automaticamente `SET app.pjb_*` em toda sessão JDBC.
Nesta fase, o read model seguro já existe e já é testado; a próxima rodada natural é materializar esses settings por conexão e boundary transacional.
