# PJB - Consulta Pública + Cockpit Pessoal Integrado 2026

## Objetivo
A consulta pública passou a respeitar duas trilhas duras e separadas:

- terceiro não autenticado ou sem vínculo pessoal: apenas processos públicos, resumo institucional, movimentação pública e atos judiciais públicos estritamente permitidos
- titular autenticado ou operador autorizado: entrada pessoal com conectores vivos para calendário, prazo real, calculadora judicial, etiquetas cromáticas, notas privadas e assistência contextual

## O que foi conectado neste round

### 1. Cockpit pessoal dentro do workspace híbrido
O payload de `GET /api/v1/public/consultas-publicas/workspace` agora entrega `personalHub` quando existir contexto pessoal.

Esse bloco consolida:

- métricas rápidas do acervo próprio
- quick actions para meus processos, calendário, calculadora e etiquetas
- catálogo de módulos integrados já existentes no PJB
- avisos de governança de sigilo
- trilha pronta para frontend mobile-first

### 2. Cards pessoais enriquecidos
Cada processo próprio agora pode sair com:

- `colorBand` calculada por criticidade
- etiquetas reais do workspace com `corHex`
- ações vivas por processo
- rota para overview autenticado
- rota para calendário do processo
- rota para prazo real
- rota para IA contextual
- rota para notas privadas
- rota para etiquetas do processo

## Regra cromática aplicada
A banda visual do card foi ligada ao estado operacional:

- `CRITICAL_RED`: prazo vencido
- `ATTENTION_ORANGE`: prazo até 72h
- `ACTIVE_BLUE`: movimentação muito recente
- `TAGGED_PURPLE`: organização por etiqueta sem urgência imediata
- `STABLE_NEUTRAL`: processo estável sem gatilho crítico

## Integrações reaproveitadas do PJB
Nada foi duplicado em regra de negócio. O workspace só expõe conectores para superfícies já existentes:

- `/api/v1/processos/pessoais/{processoId}/overview`
- `/api/v1/calendar/workspace?from={from}&to={to}&processoId={processoId}`
- `/api/v1/calendar/panel?from={from}&to={to}&processoId={processoId}`
- `/api/v1/processos/{processoId}/prazo-real?tipoAto=ATO_PROCESSUAL`
- `/api/v1/processual/calculos/workspace`
- `/api/v1/processual/calculos/workspace/{dominio}/ajuda`
- `/api/v1/chat/processo/{processoId}`
- `/api/v1/processos/{processoId}/notes`
- `/api/v1/workspace/etiquetas`
- `/api/v1/workspace/processos/{processoId}/etiquetas`

## Garantia de sigilo
A camada pessoal não altera a política pública.

Permanece válido:

- terceiros não recebem autos sigilosos
- CPF público só retorna processos públicos
- nome continua com desambiguação territorial
- página pública continua restrita a despacho, decisão, sentença e acórdão efetivamente públicos
- notas, etiquetas, IA contextual e calendário só aparecem para fluxo autenticado

## Impacto de frontend
O frontend agora pode montar uma home institucional única:

- topo com busca pública
- bloco de meus processos quando autenticado
- cockpit pessoal com atalhos vivos
- cards coloridos por criticidade
- lista pública separada da lista pessoal
- sem inferência manual de endpoints espalhados

## Arquivos centrais alterados

- `ConsultaPublicaWorkspaceService`
- `ConsultaPublicaWorkspaceResponse`
- `ConsultaPublicaWorkspaceRoutesDto`
- `ConsultaPublicaPersonalProcessCardDto`
- `ConsultaPublicaPersonalProcessTagDto`
- `ConsultaPublicaPersonalWorkspaceHubDto`
- `ConsultaPublicaPersonalWorkspaceSummaryDto`
- `ConsultaPublicaPersonalWorkspaceModuleDto`
- `ConsultaPublicaWorkspaceServiceTest`

## Resultado prático
A consulta pública deixa de ser apenas um buscador duro e passa a ser uma porta de entrada dual:

- pública para terceiros
- operacional e inteligente para o dono do processo

Sem romper sigilo, sem duplicar serviço e sem criar regra paralela fora do PJB.
