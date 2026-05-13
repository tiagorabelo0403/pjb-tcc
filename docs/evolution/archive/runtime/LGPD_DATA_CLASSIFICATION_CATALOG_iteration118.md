# Round 118 — Catálogo explícito de classificação LGPD e smoke HTTP executável

## O que entrou

### Smoke do frontend deixou de ser placeholder textual
- `FrontendPrimaryFlowsSmokeTest` passou a executar `MockMvc` real em superfícies críticas:
  - `/api/v1/auth/passkey/options`
  - `/api/v1/auth/passkey/finish`
  - `/api/v1/public/consultas-publicas/workspace`
  - `/api/v1/public/consultas-publicas/search`
  - `/api/v1/timeline/processo/{processoId}`
  - `/api/v1/peticionamento/inicial/sessao`
  - `/api/v1/admin/custas/{custaId}`
  - `/api/v1/admin/dje/metrics/publication`
  - `/api/v1/admin/frontend-readiness/summary`

### Readiness do frontend ficou mais honesta
- `PjbBackendReadyForFrontendApplicationService.smokePack()` agora exige:
  - presença do arquivo de smoke
  - uso de `MockMvcBuilders.standaloneSetup`
  - pelo menos uma asserção `andExpect(status().isOk())`
  - presença das rotas primárias auditadas

### Catálogo explícito de classificação LGPD
- `DataClassificationCatalog`
- `DataClassificationEntry`
- `DataClassificationCategory`

### Entidades críticas já catalogadas
- `Processo`
- `DocumentoProcessual`
- `InqueritoPolicialDigital`
- `CidadaoProcessoNacionalProjection`
- `SigiloAccessRequest`
- `SigiloProcessoProofChallenge`
- `DjePublicacao`

### Testes adicionados
- `DataClassificationCatalogTest`
- `DataClassificationCatalogCoverageTest`

## Ganho técnico
- reduz a distância entre readiness declarado e smoke executável
- entrega uma primeira malha concreta para o item LGPD do diagnóstico sênior
- prepara a próxima rodada para aplicar o catálogo a RLS efetivo e relatórios de atendimento ao titular
