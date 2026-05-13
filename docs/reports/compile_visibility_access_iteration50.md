# Round 50 — compile visibility/access relink

- `Persona` was made public and its cross-package members were opened for the workspace/submission split.
- `NationalProceduralJuizadoDecision` was made public and `toMap()` was opened for cross-package procedural routing consumers.
- Cross-package imports of package-private top-level types after the patch: `0`.
- Guards remained green, including `internal_reference_drift_guard.py`.
