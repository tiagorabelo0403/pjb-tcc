# Python tooling do PJB

O Python permanece apenas como tooling transitório de qualidade e saneamento. A stack principal do PJB continua sendo Java 21 e Spring Boot.

## Estrutura

- `scripts/` — implementação real das guards e utilitários Python
- `.venv/` — ambiente virtual local do projeto, não versionado
- `bootstrap_local_env.sh` — criação e atualização do ambiente local
- `requirements.txt` — dependências do tooling Python
- `targets/` — listas de arquivos para probes direcionadas

## Uso recomendado

```bash
./tooling/python/bootstrap_local_env.sh
tooling/python/.venv/bin/python scripts/config_taxonomy_guard.py
```

Os arquivos em `scripts/` na raiz do repositório agora são apenas entrypoints de compatibilidade para preservar os comandos existentes enquanto a migração gradual para Java 21 continua.

## Probe de recuperação de compile

```bash
tooling/python/.venv/bin/python scripts/compile_recovery_probe.py
```

A probe gera stubs transitórios só durante a execução para medir se o bloqueio principal ainda está no classpath externo ou se já apareceu drift interno do repositório.

Também existe modo direcionado por lote de arquivos:

```bash
tooling/python/.venv/bin/python scripts/compile_recovery_probe.py --paths-file tooling/python/targets/compile_recovery_hotspots.txt --report-suffix targeted_hotspots
tooling/python/.venv/bin/python scripts/compile_recovery_probe.py --paths-file tooling/python/targets/compile_recovery_jurisdiction_cluster.txt --report-suffix jurisdiction_cluster
```

Os relatórios ficam em `docs/reports/compile_recovery_probe*.json` e `docs/reports/compile_recovery_probe*.md`. A classificação agora também separa ruído provável de Lombok, shape de anotação e `same-package-symbol-candidate` para não confundir fechamento parcial do lote com drift interno real.

## Docker local

```bash
tooling/python/.venv/bin/python scripts/docker_compose_guard.py
```

A guard de compose valida bind mounts, contextos de build, dockerfiles e combinações de overlays (`base`, `replica`, `ha`) antes de tentar subir a malha local.
