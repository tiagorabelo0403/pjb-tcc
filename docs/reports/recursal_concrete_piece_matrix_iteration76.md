# Round 76 — matriz concreta de peças por ator e rito

## Entradas principais
- `RecursalConcretePieceMatrixBlueprint`
- `RecursalConcretePieceMatrixTrackFactory`
- passo novo no playbook: `MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO`
- trilha nova no workspace: `MATRIZ_PECAS_CONCRETAS_POR_ATOR_E_RITO`

## Objetivo
Reutilizar a espinha de peticionamento já existente e classificar a peça concreta conforme ator jurídico e rito, evitando tratar recurso, embargos, parecer, manifestação, contrarrazões, memoriais e peças complementares como variações cosméticas da petição inicial.

## Superfícies reaproveitadas
- studio/workspace, revisão governada, diff, wizard e jornada inteligente
- participação ativa workspace/protocolar/submissões
- painéis e malhas de Defensoria, Procuradoria e MP
- laudo, quesitos, oficial de justiça e pauta/audiência
