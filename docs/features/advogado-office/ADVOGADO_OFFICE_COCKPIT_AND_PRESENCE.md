# Cockpit institucional do escritorio e presenca online

## O que entrou

- resumo institucional do escritorio ativo ou do escritorio pessoal do patrono
- presenca online por escritorio com janela operacional curta
- lista completa de membros para a tela do afiliado e do patrono
- garantia de escritorio pessoal para advocacia propria
- integracao direta com bootstrap e frontend app

## Endpoints

- `POST /api/v1/frontend/app/offices/personal/ensure`
- `GET /api/v1/frontend/app/offices/workspace/summary`

## O que o frontend recebe no resumo

- nome do escritorio
- fundador
- patrono signatario
- quantidade total de membros
- quantidade de membros ativos
- quantidade de membros online no momento operacional
- quantidade de escritorios proprios do usuario atual
- quantidade de vinculos ativos do usuario atual
- ramos permitidos
- lista completa de membros
- lista de membros online
- bloqueios institucionais
- hints operacionais

## Presenca online

A presenca e atualizada pelo backend sempre que o usuario navega em contexto de escritorio. O resumo considera online quem teve atividade recente dentro da janela operacional da equipe ativa.

## Escritorio pessoal

Quando o advogado deseja atuar por conta propria com o mesmo cockpit institucional, o frontend pode chamar o endpoint de garantia de escritorio pessoal. Se ja existir escritorio proprio, o backend reaproveita o mais recente e ativa o contexto. Se nao existir, cria um escritorio pessoal com politica institucional completa.
