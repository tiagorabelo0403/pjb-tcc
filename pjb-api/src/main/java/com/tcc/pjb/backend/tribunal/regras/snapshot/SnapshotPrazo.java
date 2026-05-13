package com.tcc.pjb.backend.tribunal.regras.snapshot;

import java.time.Instant;
import java.util.Map;

public record SnapshotPrazo(
        Map<String, Integer> prazosDias,
        int recessoInicioDezembroDia,
        int recessoFimJaneiroDia,
        Instant geradoEm
) {}
