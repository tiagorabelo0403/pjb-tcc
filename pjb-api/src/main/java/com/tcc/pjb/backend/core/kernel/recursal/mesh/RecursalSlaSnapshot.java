package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.time.LocalDate;

public record RecursalSlaSnapshot(
        RecursalLifecycleState estado,
        RecursalTribunal tribunal,
        int diasUteis,
        boolean fatalParaPartes,
        String fundamentoLegal,
        LocalDate dataReferencia,
        LocalDate dataPrevistaSaida,
        boolean vencido,
        int diasUteisExcedidos,
        String severidade) {
}
