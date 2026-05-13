# Round 76 — automação viva de audiência e recursal

## O que entrou

- enriquecimento da automação de providências da magistratura com:
  - número do processo
  - participantes e contatos vinculados
  - inbox, fila e painel-alvo
  - servidor responsável e retaguarda sugeridos
- roteamento automático para painel dedicado de audiência quando o ato marcar audiência
- reaproveitamento do work item nativo quando o ato jurisdicional já tiver criado trilha operacional própria
- projeção do mesmo modelo para fluxo recursal:
  - recebimento recursal
  - colegiado
  - acórdão
  - embargos
- distribuição automática para secretaria/célula responsável também no segundo grau e malha colegiada

## Arquivos centrais

- `MagistraturaJudicialProvidenceAutomationService`
- `RecursalOperationalAutomationService`
- `SecretariatOperationalAssignmentService`
- `SecretariatQueueProjectionService`
- `RecursalPeticionamentoFacadeService`

## Efeito prático

Quando o magistrado marca audiência, o PJB agora deriva providências com painel, fila, servidor responsável e participantes do processo.

Quando um recurso ou embargo é protocolado, o PJB também projeta a automação para a malha recursal correta, inclusive no segundo grau e trilhas colegiadas.
