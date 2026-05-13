# Round 133 - Magistratura multigrau executable evidence and execution support split

## Objetivo
Fechar a lacuna remanescente de evidência executável no eixo `/api/v1/magistratura/atos` para cenários de bloqueio, segundo grau e tribunal superior, enquanto reduz o acoplamento residual da execução material dos atos.

## O que entrou
- `MagistraturaJudicialActRelatoriaFormalizationSupport`
- `MagistraturaJudicialActPanelExecutionSupport`
- `MagistraturaJudicialActExecutionSupport` reduzido para orquestração curta dos ritos singular, relatorial e multigrau
- `MagistraturaJudicialActRelatoriaFormalizationSupportTest`
- `MagistraturaJudicialActPanelExecutionSupportTest`
- `MagistraturaJudicialActExecutionRefinementArchitectureTest`
- ampliação de `MagistraturaJudicialActsControllerIT`
- ampliação de `MagistraturaJudicialActsControllerProviderContractTest`
- ampliação do pact `PjbMagistraturaActsConsumer-PjbMagistraturaActsProvider.json`

## Evidência executável nova
- preview bloqueado por guard rail com `allowed=false` e `verdict=BLOCK`
- execução bloqueada com `403` sem mascarar a negação jurisdicional
- execução colegiada de `VOTO_COLEGIADO` em segundo grau com lane `SEGUNDO_GRAU`
- execução superior de `DECISAO_PLENARIA` com lane `SUPERIOR`
- provider contracts cobrindo preview bloqueado, execução colegiada e execução superior

## Ganho estrutural
A execução material deixou de concentrar formalização relatorial e ritos colegiados/plenários no mesmo suporte, reduzindo o risco de regressão local em um eixo operacional quente da magistratura.

## Limitação honesta
O Maven Wrapper continua sem validação completa neste ambiente por falha externa de download do Maven, então a rodada foi validada por guards, inspeção estrutural, testes adicionados no código, `git diff --check` e commit local.
