# Round 74 — Atos processuais vivos da magistratura

Entrou uma malha unificada de atos jurisdicionais para juiz, desembargador e ministro, sem duplicar painel por carreira e sem espalhar regra de competência em múltiplos controllers.

## Superfícies novas

- `GET /api/v1/magistratura/atos`
- `GET /api/v1/magistratura/processos/{processoId}/atos/preview?action=...`
- `POST /api/v1/magistratura/processos/{processoId}/atos`

## Núcleo novo

- `MagistraturaJudicialActWorkbenchService`
- `MagistraturaJudicialActsController`
- `MagistraturaJudicialActCode`
- `MagistraturaJudicialActWorkspaceResponse`
- `MagistraturaJudicialActPreviewResponse`
- `MagistraturaJudicialActCommandRequest`
- `MagistraturaJudicialActCommandResponse`

## Cobertura de atos

### Primeiro grau
- despacho
- decisão interlocutória
- sentença
- designação de audiência
- ordem de cumprimento ao oficial
- certidão de trânsito em julgado
- nomeação de perito

### Segundo grau
- despacho da relatoria
- decisão monocrática
- voto colegiado
- acórdão
- pedido de vista
- destaque
- nomeação de perito

### Superior
- decisão monocrática
- inclusão em pauta
- decisão plenária
- nomeação de perito

## Reuso estrutural

Os atos nativos já existentes continuam sendo executados pelos serviços próprios do projeto:

- `JuizGabineteDecisionalService`
- `JuizOficialCumprimentoOrderService`
- `CertidaoTransitoJulgadoService`
- `DesembargadorColegialdoPainelService`
- `MinistroPlenarioService`
- `PeritoNomeacaoService`

Os atos de relatoria que ainda não tinham uma superfície única foram formalizados no workbench com reuso de:

- `InstitutionalActorRoutingService`
- `RecursalQualifiedDocumentMaterializerService`
- `DecisionSafetyService`
- `CaseContinuityDecisionGateService`
- `JuizProcessoGuardRailService`

## Guard rails

A malha nova respeita a trilha de magistratura do usuário e, quando o ato exige aderência material do processo, aplica o guard rail jurisdicional antes da execução.

Também foi acrescentada proteção no `InstitutionalCriticalActionHttpGuardFilter` para a rota unificada de atos da magistratura.

## Resultado

O PJB passa a ter um workspace vivo de despachos, decisões, decisões interlocutórias, sentenças, nomeações e atos colegiados/plenários, tanto estadual quanto federal e superior, sem duplicar backend por cargo e sem abrir uma rota poderosa fora da cerca crítica institucional.
