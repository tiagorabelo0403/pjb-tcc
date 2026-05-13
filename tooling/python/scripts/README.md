# Organização do tooling Python

## Guards
- `architecture_hygiene_guard.py`
- `constructor_injection_guard.py`
- `runtime_concurrency_guard.py`
- `transactional_hotspot_guard.py`
- `config_taxonomy_guard.py`
- `repository_layout_guard.py`
- `internal_reference_drift_guard.py`

## Probes de recuperação
- `compile_recovery_probe.py` — sonda heurística para compile recovery
- `compile_recovery_support.py` — catálogo de stubs e regras de classificação de símbolos

## Targets auxiliares
- `../targets/compile_recovery_hotspots.txt` — lote inicial de arquivos para probe direcionada

O objetivo deste diretório é manter o tooling transitório separado por função, sem contaminar a fachada principal do repositório.
