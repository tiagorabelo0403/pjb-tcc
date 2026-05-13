# Round 146 - refinamento da participação ativa processual

## Objetivo
Reduzir o hotspot estrutural de `ProcessualParticipacaoAtivaFacadeService` sem alterar a borda pública do workspace e do protocolo interno.

## O que entrou
- `ProcessualParticipacaoAtivaWorkspaceSupport`
- `ProcessualParticipacaoAtivaSubmissionSupport`
- `ProcessualParticipacaoAtivaSupportUtils`
- `ProcessualParticipacaoAtivaWorkspaceSupportTest`
- `ProcessualParticipacaoAtivaSubmissionSupportTest`
- `ProcessualParticipacaoAtivaFacadeRefinementArchitectureTest`

## Decomposição executada
A facade deixou de concentrar:
- catálogo de ações por persona/fase
- pendências por work item e inferência por fase
- roteamento topológico do workspace
- política de assinatura
- guarda representativa
- guarda de segurança/sigilo
- guarda de prazo
- diferencial de experiência do workspace
- preparação do documento principal
- preparação/validação de anexos
- descrição de recepção e resumo de ACK

## Resultado
- `ProcessualParticipacaoAtivaFacadeService`: 1242 -> 370 linhas
- a facade ficou restrita à orquestração, validação final, abertura do work item de recepção e append de eventos
- a malha semântica do workspace foi movida para `ProcessualParticipacaoAtivaWorkspaceSupport`
- a malha documental de submissão foi movida para `ProcessualParticipacaoAtivaSubmissionSupport`

## Evidência adicionada
- teste de comportamento para anexos duplicados por hash no mesmo lote
- teste de comportamento para classificação sensível do documento principal
- teste de comportamento para guarda sigilosa e representação privada
- trava arquitetural para impedir reabsorção da heurística removida na facade

## Observações honestas
- o Maven Wrapper continua sem prova completa neste ambiente por falha externa no download do `apache-maven-3.9.6-bin.zip`
- a rodada foi validada por inspeção estrutural, guards Python, testes adicionados no código, `git diff --check` e commit local
