# Baseline de modularizacao

## 1. Violacoes antigas conhecidas

O PJB possui divida arquitetural historica que nao deve ser corrigida em uma unica rodada:

- Controllers e services legados importando repositories.
- Pacotes compartilhados amplos em `model`, `repository`, `service` e `controller`.
- Modulos parciais em `modules.*` usando `controller/service/repository/entity/dto`.
- Ciclo antigo entre `modules.advocacia`, `modules.laiane` e `modules.auditoria`.
- Regras ArchUnit gerais desabilitadas para baseline legado.
- Uso de `findAll` em services e fluxos de leitura sem padrao uniforme de paginacao.
- DTOs e entities legadas convivendo fora de bounded contexts explicitos.

## 2. Violacoes toleradas temporariamente

Sao toleradas temporariamente:

- Dependencias legadas em `com.tcc.pjb.backend.controller`, `service`, `model` e `repository`.
- Modulos historicos que ainda nao seguem `domain/application/infrastructure/web/api`.
- Dependencias reciprocas historicas entre `advocacia`, `laiane` e `auditoria`, ate a criacao de facades.
- Repositories internos em modulos legados ate existir facade ou port.
- `findAll` em consultas administrativas pequenas, desde que avaliadas em onda propria.

## 3. Violacoes que nao podem aumentar

Nao devem ser aceitas em novos modulos:

- Controller acessando repository.
- Domain dependendo de Spring, JPA, web ou infrastructure.
- Application dependendo de web.
- Infrastructure dependendo de web.
- Outro modulo acessando repository interno.
- Entity JPA em `domain`.
- Repository em `domain`.
- Ciclo entre modulos.
- `findAll` novo em service/job sem limite claro.

## 4. Como detectar novas violacoes

- `scripts/modular_monolith_guard.py`
- `ModularMonolithArchitectureTest`
- Revisao de imports no diff.
- Relatorio de guard em `docs/reports/modular_monolith_guard_report.md`.

## 5. Como reduzir por ondas

- Identificar um fluxo pequeno.
- Criar facade/port antes de mover classe.
- Criar teste de comportamento.
- Migrar uma dependencia por vez.
- Rodar guard e ArchUnit.
- Registrar resultado no relatorio da onda.

## 6. Meta de reducao

Metas iniciais:

- Novos modulos com zero violacao de camada.
- Reduzir controllers com repository direto por contexto migrado.
- Substituir `findAll` em fluxos produtivos por consultas paginadas ou read models.
- Aumentar uso de ports/facades para processo, usuario, documento, movimentacao e auditoria.

## 7. O que nao sera corrigido agora

Nao sera corrigido nesta rodada:

- Movimentacao em massa de pacotes.
- Renomeacao em massa de classes.
- Separacao completa de `model`.
- Substituicao global de repositories por ports.
- Eliminacao completa de `findAll`.
- Reescrita de controllers legados.
- Introducao obrigatoria de Spring Modulith.

## 8. Por que corrigir tudo agora seria perigoso

Corrigir tudo agora criaria uma mudanca grande demais, dificil de revisar, com risco de quebrar API, migrations, wiring Spring e testes existentes. A abordagem segura e bloquear aumento da divida, criar guardas e migrar por ondas com evidencia.
