# Round 145 — refinamento da distribuição processual nacional

## Estado consolidado
- a `DistribuicaoProcessualNacionalEngine` deixou de concentrar, no mesmo arquivo, a classificação do trilho especializado de distribuição e a adaptação/inferência derivada de `Processo`
- entraram dois suportes explícitos:
  - `DistribuicaoProcessualTrackSupport`
  - `DistribuicaoProcessualProcessoSupport`
- a engine principal foi preservada como borda de orquestração da avaliação nacional, snapshot inicial, redistribuição por impedimento, consulta e diagnóstico
- o hotspot caiu de 1214 linhas para 860 linhas

## O que saiu da engine principal
- resolução do trilho especializado
- alertas/fundamentos/checklist especializados por contexto
- segmentação de fila e inbox por track
- inferência de grau jurisdicional a partir da unidade
- inferência de área especializada a partir de rito/matéria/unidade
- adaptação de `Processo` para `DistribuicaoRequest`
- urgência e escalonamento derivados do processo
- extração de referência de prevenção/vínculo

## Artefatos principais
- `DistribuicaoProcessualTrackSupport`
- `DistribuicaoProcessualProcessoSupport`
- `DistribuicaoProcessualTrackSupportTest`
- `DistribuicaoProcessualProcessoSupportTest`
- `DistribuicaoProcessualNacionalEngineRefinementArchitectureTest`

## O que esta rodada fecha de forma concreta
- reduz mais uma god class de alto impacto operacional no eixo de distribuição nacional
- separa heurística de trilho especializado da engine que orquestra routing/governance/constraint snapshot
- separa a leitura derivada de `Processo` da engine principal
- adiciona teste de comportamento para custódia/constitucional e para derivação de urgência/grau a partir do processo
- adiciona trava arquitetural para impedir reabsorção da heurística removida

## Validação honesta
- guards Python executadas
- `git diff --check` executado
- não houve afirmação de build/teste Maven completo neste ambiente
