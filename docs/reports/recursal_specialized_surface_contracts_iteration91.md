# Round 91 — provider contracts e MockMvc dirigido para surfaces recursais especializadas

Nesta rodada a malha recursal saiu do boundary especializado sem blindagem e ganhou prova executável focada no HTTP das quatro surfaces novas.

## O que entrou

- `RecursalSpecializedSurfacesControllerProviderContractTest`
- `PjbRecursalSpecializedSurfacesConsumer-PjbRecursalSpecializedSurfacesProvider.json`
- `PjbRecursalSpecializedSurfacesProviderContractCoverageArchitectureTest`
- `RecursalSpecializedSurfaceControllersIT`

## Rotas cobertas

- `/api/v1/processual/recursal/surfaces/attorney`
- `/api/v1/processual/recursal/surfaces/institutional`
- `/api/v1/processual/recursal/surfaces/documental`
- `/api/v1/processual/recursal/surfaces/intelligence`

## O que esta blindado agora

- shape HTTP minimo das surfaces especializadas
- eixo do advogado com gap critico explicito
- eixo institucional com caixas/secretaria preservados no contrato
- eixo documental com gap documental explicito
- eixo de inteligencia com gap mobile/notificacional explicito
- cobertura arquitetural do pact para impedir regressao silenciosa das rotas

## Validacao honesta

- `runtime_concurrency_guard.py` passou
- o pact JSON foi validado estruturalmente por parser local
- nao ha afirmacao de build Maven global verde
- nao ha afirmacao de compile total do `pjb-api`
- nao ha afirmacao de Docker estavel

## Proximo passo correto

1. aprofundar viewer/autenticidade/assinatura documental soberanos mais finos
2. endurecer governanca mobile/notificacional sem scheduler paralelo
3. seguir fechando a recuperacao de compile global do `pjb-api`
