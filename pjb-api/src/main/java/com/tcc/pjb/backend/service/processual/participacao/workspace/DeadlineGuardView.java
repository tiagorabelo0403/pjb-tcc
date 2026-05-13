package com.tcc.pjb.backend.service.processual.participacao.workspace;

import java.time.Instant;
import java.util.List;

public record DeadlineGuardView(String lane,
                                Instant dueAt,
                                Instant nextPendingDueAt,
                                boolean urgent,
                                String referenciaUsuario,
                                List<String> sinais) {
}
