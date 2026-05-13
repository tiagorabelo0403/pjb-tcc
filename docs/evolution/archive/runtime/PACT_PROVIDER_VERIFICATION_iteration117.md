# Round 117 — Pact provider verification real

Esta rodada substitui parte do congelamento documental do contrato HTTP por verificação executável no lado provider.

## O que entrou

- dependências de Pact provider (`junit5` e `spring6`) no `pom.xml`
- pacts versionados em `pjb-api/src/test/resources/pacts/provider`
- provider verification real para:
  - autenticação (`PasskeyAuthControllerProviderContractTest`)
  - peticionamento (`PeticionamentoControllerProviderContractTest`)
  - consulta pública (`ConsultasPublicasControllerProviderContractTest`)
- `quality-gates.yml` atualizado para executar a nova suíte
- `PjbQualityGateReadinessApplicationService` passou a diferenciar presença de consumer e provider verification

## Objetivo arquitetural

Fechar a lacuna entre:

- freeze documental de contrato público
- verificação automatizada do contrato realmente servido pelos controllers críticos

## Limites honestos desta rodada

- a verificação ainda cobre três superfícies prioritárias, não os 361 controllers
- os pact files estão versionados localmente; ainda não há broker central nem publicação de resultados
- a cobertura continua focada em shape HTTP/controlador, não em fluxo end-to-end com banco real

## Próximos alvos coerentes

1. expandir provider verification para consulta pública de busca e detalhe de processo
2. adicionar smoke tests de ponta a ponta para frontend primário
3. amarrar publicação/consumo dos pacts em pipeline dedicado
