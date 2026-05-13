# Consulta pública + cockpit pessoal integrado 2026

## O que entrou

- Novo endpoint autenticado `GET /api/v1/processos/pessoais/cockpit`
- Integração direta entre:
  - meus processos
  - overview autenticado
  - calendário processual
  - prazo real
  - timeline de movimentação
  - etiquetas e cores
  - notas privadas
  - calculadora judicial
  - IA contextual por processo

## Objetivo

Fechar a lacuna entre a consulta pública e a operação pessoal do titular. A tela pública continua restrita para terceiros, mas o usuário autenticado passa a receber uma superfície única para abrir o processo certo e saltar para calendário, cálculo, IA, prazo real e leitura orientada sem reconstruir contexto no frontend.

## Endpoint novo

`GET /api/v1/processos/pessoais/cockpit?processoId={id}&from=2026-04-13&to=2026-05-14`

### Payload principal

- `portfolio`: resumo do portfólio pessoal já calculado no workspace
- `portfolioCalendar`: digest agregado do calendário
- `portfolioMovement`: digest agregado de movimentação, prazo e bloqueio
- `spotlight`: processo em foco com overview, prazo real, timeline, notas e etiquetas
- `calculatorHints`: domínios recomendados da calculadora conforme o ramo do processo
- `aiAssist`: prompts sugeridos e guardrails para IA contextual
- `quickActions`: rotas prontas para frontend mobile-first

## Segurança

- guarda de acesso pessoal reforçada com `PersonalProcessAccessGuardService`
- o cockpit só opera em modo autenticado
- processo em foco continua exigindo vínculo civil direto
- nada disso amplia a visibilidade pública de terceiros

## Ajustes complementares

- `ConsultaPublicaWorkspaceRoutesDto` agora expõe `personalCockpit`
- `ConsultaPublicaWorkspaceService` passou a anunciar o cockpit pessoal como rota explícita
- `ProcessoNoteRepository` agora expõe `countByProcessoId` para digest operacional

## Ganho prático para o frontend

- home única do usuário autenticado
- processo em foco pronto para leitura e ação
- menos chamadas soltas para descobrir estado operacional
- melhor orquestração de cor processual, prazo, timeline e IA
