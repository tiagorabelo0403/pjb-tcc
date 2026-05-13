# Round 71 — escalonamento de alertas por perfil e criticidade

## Objetivo
Especializar o eixo de alertas recursais para dizer quem recebe cada aviso, com qual criticidade e em quais superfícies já existentes do PJB, sem criar central paralela.

## O que entrou
- `RecursalNotificationAudienceBlueprint`
- `RecursalNotificationAudienceTrackFactory`
- passo de playbook `ESCALONAR_ALERTAS_POR_PERFIL_E_CRITICIDADE`
- trilha de workspace `ESCALONAMENTO_ALERTAS_POR_PERFIL`
- labels formais:
  - `DEGRAUS_NOTIFICACAO_POR_PERFIL`
  - `CRITICIDADE_PRAZO_RECURSAL`
  - `AVISOS_CIDADAO_PROCESSO_PROPRIO`
  - `AVISOS_REPRESENTACAO_TECNICA`
  - `AVISOS_SECRETARIA_MAGISTRATURA`

## Conexões reaproveitadas
- cidadão: `/api/v1/cidadao/processos`, overview, timeline visual e preview de notificação;
- representação: dashboards executivos profissional/escritório/Defensoria/Procuradoria e dispatch multicanal;
- operação interna: painel temporal, pendências processuais, quick-actions institucionais e preview de automação de magistratura.

## Regra operacional
- cidadão: só processo próprio, aviso externo, sem detalhe tático excessivo;
- representação técnica: aviso tático de interposição, contrarrazões, adesivo, preparo, pauta e publicação;
- secretaria/magistratura: aviso operacional com criticidade reforçada para risco de perda de janela, pauta próxima e publicação relevante;
- criticidade alta para preparo insuficiente, feriado local não comprovado e ramos mais sensíveis.
