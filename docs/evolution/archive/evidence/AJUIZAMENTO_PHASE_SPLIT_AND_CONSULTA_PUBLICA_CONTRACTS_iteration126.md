# Round 126 — phase split do ajuizamento e expansão contratual da consulta pública

## O que entrou

Esta rodada atacou dois déficits ainda abertos da base:

- retenção de side effects operacionais dentro do command path síncrono de `AjuizamentoService`
- cobertura contratual e HTTP executável ainda parcial da surface de consulta pública

Entrou de forma concreta:

- `AjuizamentoService` deixou de segurar no command path central:
  - `MapaCompetenciaDinamicoEngine`
  - `ProcessoInitialDistributionSnapshotService`
  - `ProntuarioNacionalService`
  - `FederalismoJudicialEngine`
  - `PainelNacionalJusticaService`
  - `RadarPadroesService`
- novo listener pós-commit `AjuizamentoPostCommitOperationalEffectsService`
- novo budget explícito do listener pós-commit:
  - `ajuizamento.service.post-commit.persist`
- novo teste unitário:
  - `AjuizamentoPostCommitOperationalEffectsServiceTest`
- nova trava arquitetural:
  - `PjbAjuizamentoPhaseSplitArchitectureTest`
- expansão real de Pact provider verification da consulta pública para:
  - workspace
  - search
  - detail
  - resolve page
- `ConsultasPublicasControllerProviderContractTest` ampliado com múltiplos estados/contratos
- `PjbConsultaPublicaConsumer-PjbConsultaPublicaProvider.json` ampliado com 4 interações
- novo IT HTTP real:
  - `ConsultasPublicasControllerIT`
- nova trava arquitetural:
  - `PjbConsultaPublicaSurfaceArchitectureTest`
- `docs/openapi/public-api.yaml` atualizado com:
  - `/api/v1/public/consultas-publicas/workspace`
  - `/api/v1/public/consultas-publicas/processos/{numero}`

## O que esta rodada passou a provar

### Ajuizamento

O serviço central de ajuizamento agora fica mais honesto como command path transacional curto:

- persiste o processo
- emite o outbox
- publica o evento de pós-commit

Efeitos operacionais de projeção e integração interna passaram a rodar no listener pós-commit governado, reduzindo retenção de conexão e acoplamento do command path principal.

### Consulta pública

A superfície pública passou a ter evidência executável mais forte em três camadas ao mesmo tempo:

- provider contract real para workspace, search, detail e resolve page
- integração HTTP real via `MockMvc` com banco Postgres/Testcontainers
- OpenAPI estático mais honesto com rotas que já existiam no controller, mas ainda não apareciam na trilha documental pública

## Ganho estrutural

Este round reduz a distância entre:

- command path principal e side effects operacionais do ajuizamento
- controller público real e contrato provider/OpenAPI da consulta pública

A trilha deixa de depender apenas de leitura de código e passa a ter:

- separação pós-commit mais clara no ajuizamento
- contrato executável mais amplo para consumidores da consulta pública
- prova HTTP real de busca, detail, workspace e resolução de página pública

## Limitação honesta

O `AjuizarProcessoCommand` ainda concentra IA, auditoria e conector judicial na mesma fronteira transacional principal. O phase split desta rodada fechou o `AjuizamentoService`, mas não esgotou o command path alternativo mais pesado.

Na consulta pública, a cobertura contratual cresceu bastante, mas ainda cabe ampliar para outras superfícies públicas correlatas e para pactos adicionais de erro/negação/mascara.

## Próximo alvo recomendado

- phase split real do `AjuizarProcessoCommand` com extração pós-commit de IA/auditoria/integração externa
- ampliar Testcontainers em fluxo multipart/controlador de ajuizamento
- aprofundar bounded context isolation e N+1 hot paths em workbench institucional e magistratura
