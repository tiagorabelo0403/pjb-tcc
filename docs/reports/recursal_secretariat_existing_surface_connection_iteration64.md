# Round 64 — conexão explícita da matriz recursal com superfícies já existentes

## Objetivo

Parar de tratar a matriz de capacidades da secretaria multigrau como descrição abstrata e conectá-la às superfícies já existentes do projeto, evitando contrato paralelo no eixo recursal.

## O que entrou

- ampliação de `RecursalSecretariatCapabilityMatrixBlueprint` para reutilizar explicitamente:
  - `secretariat/queue/panel`
  - `secretariat/queue/agenda`
  - `secretariat/queue/governance`
  - `secretariat/queue/coverage`
  - `secretariat/queue/formal-catalog`
  - `secretariat/operacional/snapshot`
- conexão explícita com superfícies colegiadas já existentes:
  - pauta
  - publicação
  - sustentação oral
  - acórdão
  - baixa à origem
- conexão explícita com superfícies institucionais já existentes:
  - `institutional-support/{branchCode}/competence-matrix`
  - `institutional-support/{branchCode}/coverage`
  - `institutional-support/{branchCode}/processos/{processoId}/pre-pauta`
- reaproveitamento de rotas especializadas por ramo já existentes:
  - trabalhista
  - eleitoral
  - militar
  - trilha civil/penal/julgamentos
- ampliação do `RecursalAutomationPlaybookService` com o passo `REUSAR_SUPERFICIES_EXISTENTES`

## Resultado estrutural

A trilha recursal deixou de falar apenas em “secretaria multigrau” e passou a mostrar, no próprio playbook e na própria matriz, quais APIs já existentes devem ser reutilizadas em vez de criar novo endpoint satélite.

## Validação honesta

- guards Python executadas com sucesso
- smoke compile local com `javac` executado para a rota que envolve `REUSAR_SUPERFICIES_EXISTENTES`
- sem afirmação de build Maven global verde
- sem afirmação de compile total do `pjb-api`
