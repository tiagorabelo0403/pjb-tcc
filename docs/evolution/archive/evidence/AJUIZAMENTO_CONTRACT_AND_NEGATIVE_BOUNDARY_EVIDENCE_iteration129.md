# Round 129 - Ajuizamento contract expansion and negative boundary evidence

## Objetivo
Fechar uma fase de maior impacto no bounded context de ajuizamento combinando duas lacunas que ainda estavam parciais:

- provider verification ainda muito pequena na superfície de preparação/roteamento do ajuizamento
- evidência executável do boundary HTTP ainda concentrada demais no caminho feliz

A rodada foi desenhada para aumentar contrato verificável e, ao mesmo tempo, provar que o ajuizamento não degrada de forma catastrófica quando o side effect externo falha após o commit.

## O que entrou

### 1. Pact/provider coverage ampliada no AjuizamentoIntentController
O arquivo `AjuizamentoIntentControllerProviderContractTest` deixou de verificar apenas a listagem de ramos e passou a cobrir também:

- `GET /api/v1/ai/ajuizamento/ramos/{ramo}`
- `POST /api/v1/ai/ajuizamento/route`
- `POST /api/v1/ai/ajuizamento/infer-map`
- `POST /api/v1/ai/ajuizamento/catalog/tribunais/capabilities`

O pact versionado `PjbAjuizamentoIntentConsumer-PjbAjuizamentoIntentProvider.json` foi ampliado para refletir essas superfícies.

### 2. Trava arquitetural da cobertura contratual de ajuizamento
Entrou `PjbAjuizamentoProviderContractCoverageArchitectureTest`, garantindo que o pact do ajuizamento continue cobrindo:

- listagem de ramos
- detalhe de ramo
- roteamento
- infer-map
- capacidades de tribunal

Isso impede regressão silenciosa da malha contratual quando a superfície crescer.

### 3. Evidência executável negativa no boundary HTTP de ajuizamento
O `ProcessoCommandControllerIT` foi ampliado para provar:

- erro de validação com `fieldErrors` para `classe`, `materia` e `rito`
- ausência de persistência quando o payload multipart é inválido
- degradação graciosa quando o conector judicial falha no pós-commit

### 4. Persistência e auditoria preservadas com falha pós-commit
Foi adicionada prova HTTP real de que:

- o request de ajuizamento continua retornando sucesso
- o processo segue persistido
- a auditoria imutável segue registrada
- a falha externa no conector não contamina a transação principal

Isso reduz risco de retenção transacional indevida e evita que instabilidade externa produza rollback cosmético do que já foi commitado corretamente.

## Efeito sobre o diagnóstico sênior

### Pact provider verification insuficiente
Melhorou de forma concreta. A cobertura continua longe do ideal para a escala total da base, mas deixou de ser um contrato quase simbólico dentro do eixo de ajuizamento.

### Razão teste/produção baixa
Melhorou qualitativamente com testes de comportamento mais relevantes. Não resolve a razão total da base, mas aumenta a densidade de evidência executável em um caminho crítico.

### Resiliência insuficiente nas integrações externas
A rodada reforça a postura de resiliência já iniciada no conector judicial ao provar, por teste HTTP real, que a falha externa pós-commit não derruba a operação central.

## Limitações honestas
- o Maven Wrapper continua bloqueado pelo download externo do Maven neste ambiente
- por isso esta rodada foi validada por inspeção estrutural, guards Python, diff limpo, commit local e testes adicionados no código, mas não por execução completa do build Maven neste container

## Próximo alvo natural
- ampliar provider contracts do ajuizamento para superfícies mais próximas do protocolo real e cenários negativos mais ricos
- expandir os cenários HTTP/Testcontainers do ajuizamento para anexação múltipla, rejeições estruturais mais profundas e pós-commit adicional
- continuar mitigação de N+1 e crescimento de evidência executável em painéis institucionais e workbenches quentes
