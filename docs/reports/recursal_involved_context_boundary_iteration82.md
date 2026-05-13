# Round 82 — fronteira entre envolvimento real e busca neutra

## O que entrou
- `RecursalInvolvedContextBoundaryBlueprint`
- `RecursalInvolvedContextBoundaryTrackFactory`
- novo passo do playbook: `LIMITAR_COMUTACAO_A_ENVOLVIDOS_E_PRESERVAR_BUSCA_NEUTRA`
- nova trilha do workspace: `FRONTEIRA_ENVOLVIMENTO_E_BUSCA_NEUTRA`

## Efeito estrutural
- a comutação contextual por rito/tribunal/perfil também passa a valer explicitamente para painéis próprios de recurso e embargos
- a comutação fica limitada a usuários com vínculo real aos autos
- busca genérica de processos e visualização de processo alheio permanecem neutras, reaproveitando a superfície pública já existente

## Ajustes adicionais
- `RecursalAutomationRequest` ganhou os eixos opcionais `usuarioEnvolvidoNosAutos` e `consultaProcessualGenerica`, preservando compatibilidade com construtores anteriores
