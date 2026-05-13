# Round 134 - refinamento da automação de providências da magistratura

## Objetivo
Reduzir o hotspot remanescente em `MagistraturaJudicialProvidenceAutomationService` sem regredir a API pública já endurecida em `magistratura/atos`, preservando os fluxos de preview e dispatch e mantendo a automação alinhada com o reaproveitamento da malha operacional existente.

## O que entrou
- `MagistraturaJudicialProvidencePlanningSupport`
- `MagistraturaJudicialProvidenceDispatchSupport`
- `MagistraturaJudicialProvidenceAutomationRules`
- `MagistraturaJudicialProvidenceAutomationRefinementArchitectureTest`
- ajuste de `MagistraturaJudicialProvidenceAutomationServiceTest`

## Resultado estrutural
- `MagistraturaJudicialProvidenceAutomationService`: `1268 -> 54` linhas
- planejamento e roteamento das providências extraídos para um suporte dedicado
- despacho operacional, reaproveitamento de `WorkItem`, score, tags e metadata extraídos para um suporte dedicado
- regras puras compartilhadas centralizadas em um artefato próprio, evitando duplicação entre preview e dispatch

## O que foi preservado
- API pública de `preview(...)`
- API pública de `dispatch(...)`
- reaproveitamento de `WorkItem` quando o ato já nasce com fila nativa
- projeção de autoridade em primeiro, segundo grau e superior
- metadata operacional para fila, retorno e trilha de confirmação

## O que esta rodada fecha de forma concreta
- reduz mais um god service no eixo da magistratura
- separa planejamento/roteamento de providências da materialização operacional na secretaria
- evita reabsorção do hotspot com trava arquitetural explícita
- mantém a automação compatível com budgets transacionais, governança assíncrona central e reaproveitamento da malha operacional

## Limitação honesta
O Maven Wrapper continua sem conseguir baixar `apache-maven-3.9.6-bin.zip` neste ambiente. A validação local permaneceu baseada em guards Python, inspeção estrutural, testes adicionados no código, `git diff --check` e commit local temporário.
