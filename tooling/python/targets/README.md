# Targets da compile recovery probe

Arquivos `.txt` com listas de fontes Java relativas à raiz do repositório.

Uso:

```bash
tooling/python/.venv/bin/python scripts/compile_recovery_probe.py \
  --paths-file tooling/python/targets/compile_recovery_jurisdiction_cluster.txt \
  --report-suffix jurisdiction_cluster
```

Targets atuais:
- `compile_recovery_hotspots.txt` — lote amplo inicial de hotspots do `pjb-api`
- `compile_recovery_jurisdiction_cluster.txt` — cluster de jurisdição e rito para reduzir drift de imports/shape
- `compile_recovery_prazos_calendar_cluster.txt` — cluster de prazos e calendário para reduzir drift entre cálculo, calendário forense e auditoria
- `compile_recovery_processual_communication_cluster.txt` — cluster de DTOs e superfícies de comunicação processual para reduzir drift de imports após a repartição por subdomínio

- `compile_recovery_processual_communication_cluster.txt` — cluster de recuperação do eixo de comunicação processual, incluindo controladores e as facades/assemblers já repartidos entre flow, governance, panel e surface
- `compile_recovery_processual_root_cluster.txt` — cluster de aplicação raiz e controladores processuais reorganizados para reduzir drift em package declaration, imports e localização de superfície

- `compile_recovery_processual_controller_cluster.txt` — superfície processual reorganizada por capacidade funcional, para sondar imports e referências após a limpeza do pacote raiz `controller/processual`
- `compile_recovery_processual_substituicao_cluster.txt` — cluster de DTOs, serviços, controladores e base operacional da substituição nacional, para sondar drift de imports após a repartição de arquitetura, sincronização, governança federativa, cockpit, execução, programa, migração, homologação, legados e tribunal
