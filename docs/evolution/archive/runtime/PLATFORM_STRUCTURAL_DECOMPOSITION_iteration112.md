# Platform Structural Decomposition — Round 112

## Objetivo
Atacar o hotspot `SecretariatQueueQueryService` sem reintroduzir fronteiras assíncronas cruas, sem misturar responsabilidades de painel/agenda/row e corrigindo um drift de compilação já presente no filtro de agenda.

## O que entrou

### 1. Decomposição do query service da secretaria
Extrações realizadas:

- `SecretariatQueuePanelRowProjectionSupport`
- `SecretariatQueuePanelProjectionSupport`
- `SecretariatQueueAgendaProjectionSupport`

O `SecretariatQueueQueryService` deixou de concentrar:

- projeção da row operacional da secretaria
- agrupamento do painel por processo/rito/vara/data
- contratos de ação do painel
- projeção da agenda operacional
- agrupamentos por filtros operacionais da agenda
- montagem de venue/notificação/completion/checklist/contacts
- filtros operacionais da agenda e buckets de prazo

### 2. Correção de compatibilidade do filtro de agenda
Correção aplicada em:

- `SecretariatQueueController`
- `SecretariatQueueQueryServicePanelTest`

Referências antigas a `SecretariatQueueQueryService.AgendaFilter` foram ajustadas para o tipo real `SecretariatQueueAgendaFilter`.

### 3. Trava arquitetural
Teste novo:

- `SecretariatQueueQueryServiceRefinementArchitectureTest`

Garantias:

- `SecretariatQueueQueryService` permanece abaixo do limiar de hotspot de service
- a decomposição em suportes de row/painel/agenda não regride silenciosamente

## Resultado objetivo

- `SecretariatQueueQueryService`: **1572 -> 470 linhas**
- o service saiu do relatório `architecture_hygiene_guard`
- o service saiu do relatório `constructor_injection_guard` por deixar de combinar tamanho + injeção em faixa crítica

## Validação executada

Executado com sucesso no ambiente da rodada:

- `python3 scripts/architecture_hygiene_guard.py`
- `python3 scripts/constructor_injection_guard.py`
- `python3 scripts/runtime_concurrency_guard.py`
- `python3 scripts/transactional_hotspot_guard.py`

Resultados relevantes:

- `SecretariatQueueQueryService` não aparece mais nos relatórios de hotspot estrutural
- sweep de concorrência continua sem `CompletableFuture.*Async` cru fora das raízes autorizadas

## Limitação honesta
Não houve build Maven completo nesta rodada porque o ambiente continua sem resolução externa do Maven Wrapper.

## Próximo alvo sugerido

1. `TransitoJulgadoArquivamentoEngine` no eixo de construtor/dependências
2. `PeticionamentoSessaoFacadeService`
3. `NationalCommunicationInstitutionalSurfaceFacadeService`
