# Round 138 - refinamento do AjuizamentoIntentEngine

## Objetivo
Reduzir a concentração heurística do `AjuizamentoIntentEngine`, isolando a malha de classificação jurídico-procedural em suporte dedicado sem abrir regressão de concorrência, sem criar fronteiras assíncronas cruas e sem alterar a API pública de inferência.

## Mudança material
- entrou `AjuizamentoIntentClassificationSupport`
- o suporte passou a concentrar:
  - inferência de esfera
  - inferência de ramo
  - inferência de sub-ramo
  - inferência de rito
  - mapeamentos auxiliares por ramo/sub-ramo/rito
- o `AjuizamentoIntentEngine` foi reduzido a borda orquestradora da inferência consolidada, preservando:
  - canonical selection
  - routing analysis
  - composição do `AjuizamentoIntent`
  - alertas/campos/documentos/passos derivados
  - recalibração de confiança

## Evidência adicionada
- `AjuizamentoIntentClassificationSupportTest`
- `AjuizamentoIntentEngineRefinementArchitectureTest`

## Garantias desta rodada
- não houve abertura de `CompletableFuture` cru
- não houve regressão em budgets transacionais
- o construtor do engine permaneceu curto
- a malha de classificação saiu do service principal e ficou travada contra reabsorção por teste arquitetural
