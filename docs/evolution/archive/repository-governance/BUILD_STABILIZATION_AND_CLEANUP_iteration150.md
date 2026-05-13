# Round 150 — Build Stabilization and Workspace Cleanup

## Scope
- stabilize local workspace/import resolution
- clean root clutter
- consolidate historical markdown notes into README
- remove local-only metadata from the delivered artifact

## Material changes
- added `spring-boot.version` to root `pom.xml`
- kept `spring-boot-configuration-processor` wired to `${spring-boot.version}` for annotation processing
- normalized the source files reported in the local compile failure list:
  - `AjuizamentoIntentClassificationSupport.java`
  - `AjuizamentoIntentEngine.java`
  - `AjuizarProcessoCommand.java`
  - `CitacaoIntimacaoExpedicaoSupport.java`
  - `PjbQualityGateReadinessApplicationService.java`
  - `PjbCoreModuleCatalogTest.java`
- added explicit `ProceduralRitoNames` import in `AjuizamentoIntentClassificationSupport`
- consolidated these root markdown files into the main `README.md` appendix and removed them from root:
  - `PETITION_STUDIO_ROUND74.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND63.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND64.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND65.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND66.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND67.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND68.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND69.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND70.md`
  - `PROFESSIONAL_FORENSIC_PANEL_ROUND71.md`
- removed local metadata from the delivered project:
  - `.git`
  - `.idea`
  - `pjb-backend-core.iml`

## Validation
- executed Python guards:
  - `architecture_hygiene_guard.py`
  - `constructor_injection_guard.py`
  - `runtime_concurrency_guard.py`
  - `transactional_hotspot_guard.py --fail-on-missing-budgets`
  - `config_taxonomy_guard.py`
- Maven Wrapper full compile was not executed in this environment because wrapper bootstrap still depends on downloading Maven externally.

## Notes
- the compile errors listed by the user came from a local workspace state that reported parser failures in `AjuizamentoIntent*`, escape issues in validation/document normalization helpers, and string literal corruption in the quality gate service. This round focused on normalizing and stabilizing the exact files named in that failure report while also cleaning the artifact structure.
