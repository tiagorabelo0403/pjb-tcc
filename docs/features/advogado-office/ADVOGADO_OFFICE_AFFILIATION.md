# Afiliacao de advogados ao escritorio

Fluxo materializado no PJB:

1. O patrono/fundador cria o escritorio.
2. O patrono convida outro advogado por email, CPF ou OAB.
3. O convite define:
   - papel na equipe
   - cargo
   - ramos permitidos
   - limite de confianca para autoassinatura
   - limite diario de autoacoes
   - bloqueio ou nao de causas pessoais
   - modo inicial ao aceitar
   - prioridade operacional do workspace
4. O advogado convidado aceita com o proprio login e com aceite expresso dos termos.
5. Se o convite for sensivel, o fluxo entra em confirmacao final obrigatoria.
6. O sistema cria/ativa o vinculo em `membros_equipe`, grava a regra de delegacao e persiste a preferencia operacional do workspace do afiliado.
7. Ao entrar em modo escritorio, a assinatura segue a politica patronal do escritorio.

## Convite padrao x convite reforcado

Convite padrao:

- aceite direto pelo advogado convidado
- ativacao imediata do vinculo
- atualizacao do contexto operacional do proprio associado

Convite reforcado:

- exigido para escopo amplo, trust alto, papel sensivel ou politica patronal endurecida
- exige pelo menos duas chaves de identificacao entre email, CPF e OAB
- exige assurance Gov.br compativel no aceite
- ao aceitar, o status muda para `AWAITING_FINAL_APPROVAL`
- a ativacao real do vinculo so ocorre na confirmacao final do escritorio

## Garantias adicionadas nesta rodada

- hash do payload do convite
- hash da identidade destinataria
- prazo de expiracao do convite
- janela de aprovacao final para convite reforcado
- idempotencia de aceite e de aprovacao final
- bloqueio de convite concorrente aberto para a mesma identidade
- workspace prioritario por vinculo
- autoativacao governada pela delegacao do escritorio
- trilha auditavel para criacao, aceite pendente e ativacao final

## Regra de contexto multi-escritorio

A sessao efetiva do usuario continua exclusiva por contexto:

- `PERSONAL`
- `OFFICE` para um escritorio especifico
- `HYBRID` para um escritorio especifico

Quando houver mais de um escritorio ativo:

- prioridade do workspace e avaliada primeiro
- autoativacao so ocorre quando houver um unico escritorio vencedor
- empate de prioridade exige selecao explicita do escritorio
- preferencias de um escritorio nao sao misturadas com outro

## Endpoints principais

- `GET /api/v1/frontend/app/offices/invites/incoming`
- `GET /api/v1/frontend/app/offices/{equipeId}/invites`
- `POST /api/v1/frontend/app/offices/invites`
- `POST /api/v1/frontend/app/offices/invites/{inviteId}/accept`
- `POST /api/v1/frontend/app/offices/invites/{inviteId}/confirm-activation`
- `POST /api/v1/frontend/app/offices/invites/{inviteId}/reject`
- `DELETE /api/v1/frontend/app/offices/invites/{inviteId}`
