# Modo Escritório do Advogado

## Objetivo

Permitir que o advogado associado, júnior ou colaborador atue no contexto do escritório do advogado sênior usando o próprio login, sem perder a possibilidade de abrir causas próprias quando a política do escritório permitir.

## Modos

- `PERSONAL`: prioriza processos próprios.
- `OFFICE`: prioriza a carteira do escritório e aplica o filtro de equipe como padrão.
- `HYBRID`: abre o escritório como contexto padrão e mantém trilha explícita para causas próprias.

## Persistência

A preferência fica em `adv_office_workspace_preference`.

Campos principais:

- usuário
- equipe preferida
- modo
- autoativação no login
- permissão para causas próprias

## Endpoints de frontend

- `GET /api/v1/frontend/app/office-mode`
- `PUT /api/v1/frontend/app/office-mode`
- `DELETE /api/v1/frontend/app/office-mode`

## Comportamento no login

Quando a preferência do advogado estiver com `auto_activate_on_login=true` e houver vínculo ativo com a equipe escolhida, o interceptor passa a resolver a equipe automaticamente mesmo sem `X-Equipe-ID` explícito.

## Resultado esperado

- advogado júnior enxerga a carteira do escritório do sênior ao entrar
- advogado pode alternar para atuação própria sem trocar de conta
- desvinculação limpa a preferência e remove a autoativação

## Escopo por ramo, confianca e certificado do patrono

A partir desta rodada, o modo escritorio passou a suportar governanca fina por integrante da equipe:

- `allowedRamos` na policy do escritorio para limitar a carteira visivel por ramo (`CIVIL`, `PENAL`, etc.)
- `allowedRamosOverride` por advogado vinculado, permitindo que o patrono entregue escopo diferente para cada associado
- `minTrustAuto` e `minTrustAutoOverride` continuam governando assinatura automatica por nivel de confianca
- `forcePatronoCertificate=true` na policy faz com que qualquer documento assinado em modo escritorio saia com o certificado do patrono/senior configurado
- fora do modo escritorio (`PERSONAL`), a assinatura volta a ser do proprio advogado executor

### Exemplos

- escritorio com policy `allowedRamos=[CIVIL, EMPRESARIAL]`
- associado junior com regra `allowedRamosOverride=[PENAL]`
- patrono configura `forcePatronoCertificate=true`

Resultado:

- no login em modo escritorio, o associado ve apenas a carteira permitida pela regra efetiva
- se o nivel de confianca nao atingir o minimo, a assinatura vai para fila do patrono
- se estiver em modo pessoal, assina com o proprio certificado


## Criação do próprio escritório e catálogo amplo de ramos

A partir desta rodada, o advogado também pode criar o próprio escritório pelo frontend.

Endpoints novos:

- `POST /api/v1/frontend/app/offices`
- `GET /api/v1/frontend/app/offices/mine`
- `GET /api/v1/frontend/app/support/catalogs/ramos-direito`

Regras:

- o advogado fundador entra como `ADVOGADO_SENIOR` e `Patrono fundador`
- o escritório nasce com policy ativa e certificado patronal vinculado ao próprio fundador
- se `allBrazilianLawEnabled=true`, o escritório nasce sem restrição de ramos, cobrindo todo o catálogo brasileiro disponível no enum `RamoDireito`
- o catálogo de ramos passou a incluir áreas materiais e processuais, como `PROCESSUAL_CIVIL`, `PROCESSUAL_PENAL`, `PROCESSUAL_TRABALHISTA`, `EXECUCAO_FISCAL`, `LICITACOES_CONTRATOS`, `REGULATORIO`, `URBANISTICO`, `CIVIL_PUBLICA_COLETIVO` e outras correlatas
