# Round 78 — comutação contextual por painel, rito, tribunal e perfil

## Objetivo
Fazer o PJB se comportar de modo diferente por painel quando o usuário entra em um processo de rito específico, preservando o shell padrão ao sair do processo e sem criar dashboards paralelos.

## Artefatos
- `RecursalPanelContextSwitchBlueprint`
- `RecursalPanelContextSwitchTrackFactory`

## Resultado
- nova trilha `COMUTACAO_CONTEXTUAL_POR_PAINEL_RITO_TRIBUNAL`;
- novo passo de playbook `COMUTAR_PAINEIS_POR_RITO_TRIBUNAL_E_PERFIL`;
- reaproveitamento explícito de superfícies de cidadão, representação, secretaria, apoio institucional e magistratura por rito/tribunal;
- restauração do shell padrão do PJB ao sair do processo ativo.
