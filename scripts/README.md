# Scripts

Entry points curtos de compatibilidade. A implementação real do tooling Python foi isolada em `tooling/python/scripts/` para não poluir a fachada principal do repositório.

## Guards obrigatórias por rodada
- `architecture_hygiene_guard.py`
- `constructor_injection_guard.py`
- `runtime_concurrency_guard.py`
- `transactional_hotspot_guard.py --fail-on-missing-budgets`
- `config_taxonomy_guard.py`
- `repository_layout_guard.py`
- `internal_reference_drift_guard.py`
- `compile_recovery_probe.py`
- `docker_compose_guard.py`
- `spring_surface_guard.py`
- `legal_ai_surface_split_guard.py`

## Utilitários complementares
- `frontend_integration_pack.py`
- `migration_alignment_report.py`
- `pdf_coverage_scan.py`
- `pdf_final_matrix.py`
- `static_sweep.py`
- `validate_end_to_end.sh`

Scripts geram relatórios em `docs/reports/`.

## Ambiente Python local do projeto
- bootstrap: `./tooling/python/bootstrap_local_env.sh`
- interpretador recomendado: `tooling/python/.venv/bin/python`
- implementação Python real: `tooling/python/scripts/`

## Targets auxiliares
- `tooling/python/targets/compile_recovery_hotspots.txt`
- `tooling/python/targets/compile_recovery_jurisdiction_cluster.txt`
- `tooling/python/targets/README.md`

A `repository_layout_guard.py` também bloqueia arquivos temporários típicos na raiz do repositório.

## Execução no Windows com política restritiva
- `pjb-api-clean-test-errors.cmd` chama o coletor PowerShell com política restrita ao processo atual, sem alterar `LocalMachine` nem `CurrentUser`.
- `pjb-error-summary.cmd` permite resumir logs sem depender de execução direta de `.ps1`.

Uso recomendado no Windows:
```powershell
.\scripts\pjb-api-clean-test-errors.cmd
```

