# Operações Processuais Governadas por Escritório

## Objetivo

Acoplar o workspace de escritório aos atos processuais sensíveis para impedir atuação fora do contexto efetivo, mesmo quando o usuário conhece o identificador do processo.

## Escopo desta rodada

- peticionamento governado com persistência própria
- recurso governado com persistência própria
- fila patronal com replay íntegro do payload
- checagem de escopo do workspace antes de peticionar
- checagem de escopo do workspace antes de assinar conteúdo processual
- enriquecimento do workspace de peticionamento com decisão operacional do processo

## Componentes

- `adv_office_process_operation`
- `AdvOfficeProcessOperation`
- `AdvOfficeProcessOperationRepository`
- `OfficeGovernedProcessOperationService`
- `AdvProcessOperationOfficeQueueExecutor`

## Regras

### Petição e recurso

Quando o processo estiver em contexto de escritório, o backend valida o escopo operacional antes de permitir o ato.

A decisão considera:

- equipe ativa
- modo efetivo do workspace
- ramos autorizados
- trust efetivo para ato sensível
- política patronal
- exigência de fila patronal

### Fila patronal

Quando a política exigir patrono, a operação não executa imediatamente.

O backend:

- persiste a operação processual com payload íntegro
- grava hash do payload
- cria item de fila patronal
- reexecuta o ato após aprovação usando o payload persistido

### Assinatura de conteúdo

A assinatura qualificada de conteúdo processual também revalida o processo no escopo do workspace.

Isso impede assinatura fora da carteira operacional visível.

## Resultado operacional

O sistema deixa de depender apenas da visibilidade da listagem.

Agora:

- a listagem é filtrada por workspace
- a decisão operacional por processo é revalidada
- o peticionamento usa a mesma governança
- a assinatura usa a mesma governança
- a fila patronal consegue replay íntegro da operação
