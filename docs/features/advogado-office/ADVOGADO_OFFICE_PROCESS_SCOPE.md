# ADVOGADO_OFFICE_PROCESS_SCOPE

Esta rodada acopla a governança do vínculo do escritório diretamente à visibilidade processual do workspace ativo.

## Objetivo

Impedir contexto misturado e impedir que o afiliado:
- veja processos fora do escritório ativo
- veja ramos não autorizados pelo vínculo
- veja processos sensíveis além do trust efetivo
- opere por ID em processo fora do escopo visível

## Regras materializadas

### 1. Workspace efetivo único
A sessão continua operando com um único contexto efetivo:
- pessoal
- escritório ativo
- híbrido com escritório ativo

### 2. Visibilidade base por ownership
Processos visíveis no workspace:
- `PERSONAL`: somente processos pessoais do usuário
- `OFFICE`: somente processos da equipe ativa
- `HYBRID`: processos da equipe ativa e, quando permitido, processos pessoais do usuário

### 3. Restrições do vínculo
No contexto de escritório, a visibilidade também respeita:
- ramos autorizados pelo vínculo/política
- trust mínimo para processos sigilosos ou ramos sensíveis
- bloqueio de causas próprias quando o vínculo assim exigir
- aviso de assinatura patronal obrigatória quando a política exigir patrono

### 4. Hardening operacional
Mesmo conhecendo o `processoId`, a operação é revalidada no backend antes da decisão de delegação/assinatura.

## Backend materializado

### Filtro Hibernate de processo
Foi criado filtro dedicado para `Processo`:
- `filtroEquipeProcesso`

Parâmetros principais:
- usuário
- equipe ativa
- inclusão ou não de casos pessoais
- ramos permitidos
- permissão para sensíveis
- bloqueio de causas próprias

### Serviço novo
- `OfficeProcessWorkspaceScopeService`

Responsabilidades:
- montar o perfil efetivo do workspace
- listar processos visíveis no workspace
- avaliar acesso operacional por processo
- endurecer operações por ID

### Integração com operação
- `OfficeDelegationService` agora exige que o processo esteja no escopo do workspace antes de decidir assinatura/queue/auto

## Endpoints novos

### Query de processos visíveis
`POST /api/v1/frontend/app/offices/workspace/processes/query`

Payload:
```json
{
  "page": 0,
  "size": 20,
  "search": "0001",
  "status": "EM_ANDAMENTO",
  "ramoDireito": "CIVIL",
  "includePersonalOwnCases": false
}
```

### Access check operacional
`GET /api/v1/frontend/app/offices/workspace/processes/{processoId}/access?action=PETICIONAR`

Retorna:
- allowed
- visibleInWorkspace
- queueRequired
- effectiveSigner
- blockers
- warnings

## Efeito prático

O afiliado deixa de operar com base apenas em "estar no escritório". Agora ele precisa estar:
- no workspace certo
- no ramo certo
- com trust compatível
- sem conflito de causa própria
- com política de assinatura compatível
