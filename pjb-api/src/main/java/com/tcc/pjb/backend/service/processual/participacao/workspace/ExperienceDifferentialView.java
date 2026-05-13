package com.tcc.pjb.backend.service.processual.participacao.workspace;

import java.time.Instant;
import java.util.List;

public record ExperienceDifferentialView(String persona,
                                         String faseAtual,
                                         List<String> destaqueAcoes,
                                         List<String> diferencias,
                                         String inboxRecepcao,
                                         Instant geradoEm) {
}
