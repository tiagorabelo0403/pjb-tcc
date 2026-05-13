# Round 73 — malotes, peticionamento e atos recursais

## Entradas principais
- `RecursalExpeditionAndProceduralActsBlueprint`
- `RecursalExpeditionAndProceduralActsTrackFactory`
- passo `ORQUESTRAR_MALOTES_PETICIONAMENTO_E_ATOS_RECURSAIS`
- trilha `MALOTES_PETICIONAMENTO_ATOS_RECURSAIS`

## Foco material
- malotes/encaminhamentos ligados ao degrau recursal
- forma própria de peticionamento para recurso e embargos
- atos recursais vivos: pauta, sessão/audiência, voto, acórdão, publicação
- intimações e chamamento de perito/oficial/terceiros reaproveitando superfícies existentes

## Superfícies reaproveitadas
- `/api/v1/peticionamento/inicial/sessao`
- `/api/v1/peticionamento/studio/workspace`
- `/api/v1/peticionamento/studio/wizard-protocolo-simples`
- `/api/v1/peticionamento/studio/jornada-inteligente`
- `/api/v1/processual/pauta-audiencia`
- `/api/v1/perito/painel`
- `/api/v1/perito/nomeacoes`
- `/api/v1/perito/operacional/snapshot`
- `/api/v1/perito/operacional/processos/{processoId}/laudo`
- `/api/v1/oficial-justica/agenda-operacional`
- `/api/v1/oficial-justica/processos-nomeados/{processoId}/workbench`
- `/api/v1/oficial-justica/processos/{processoId}/ciente-intimacao`
- `/api/v1/oficial-justica/processos/{processoId}/oficios`
- `/api/v1/oficial-justica/processos/{processoId}/oficios/resposta`

## Resultado pretendido
Manter recurso e embargos dentro do mesmo eixo operacional do PJB, com peticionamento próprio, malha de expedição formal e chamamento de auxiliares da Justiça sem módulo satélite.
