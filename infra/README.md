# Infra

Materiais operacionais do repositório.

## Estrutura

- `docker/` — imagens auxiliares e componentes locais
- `k8s/` — base, blueprints e overlays

## Observação

Arquivos `Dockerfile` e `docker-compose*.yml` permanecem na raiz por compatibilidade com
fluxos locais e testes de layout já existentes. O detalhamento operacional fica aqui.


## Docker local

- `docker-compose.yml` sobe a infraestrutura base e deixa a aplicação atrás do profile `app`
- `docker-compose.read-replica.yml` adiciona a réplica sob profile `replica`
- `docker-compose.ha.yml` adiciona borda de banco sob profile `ha`
- `docker-compose.n8n.yml` é overlay opcional sob profile `n8n` e depende do Redis da malha base
- `infra/docker/.env.compose.example` guarda um env local mínimo para compose
