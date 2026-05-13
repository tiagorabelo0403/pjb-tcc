# Round 80 — shell contextual por ator, perfil e cards de risco

## Objetivo
Fazer o PJB variar não só por rito e tribunal, mas também por ator envolvido no processo ativo, mantendo o mesmo sistema, a mesma malha de painéis e a mesma organização estrutural.

## O que entrou
- `RecursalActorContextExperienceBlueprint`
- `RecursalActorContextExperienceTrackFactory`
- passo `AJUSTAR_PERFIL_ATOR_CARDS_RISCO_E_QUICK_ACTIONS`
- trilha `SHELL_CONTEXTUAL_POR_ATOR_PERFIL`
- `perfilAtuacao` opcional em `RecursalAutomationRequest`

## Comportamento consolidado
- `CIDADAO`: overview, timeline, cor processual e aviso externo compatível com sigilo
- `REPRESENTACAO_TECNICA`: leitura, protocolo, wizard, jornada e criticidade recursal
- `DEFENSORIA`: defesa, malha, habeas corpus no penal, prazo e protocolo assistido
- `PROCURADORIA`: malha, recurso, parecer e resposta institucional
- `MINISTERIO_PUBLICO`: painel, malha, manifestação, parecer e recurso
- `SECRETARIA`: queue, governance, snapshot, juntada, intimação e conclusão
- `MAGISTRATURA`: workspace, preview, pendências, voto e decisão

## Resultado
O shell do processo ativo fica mais tecnológico e menos genérico: ele passa a resolver o perfil do ator, aplicar um `profileCode` determinístico e reorganizar cards de risco, quick actions, linguagem operacional e observabilidade sem abrir subsistema paralelo.
