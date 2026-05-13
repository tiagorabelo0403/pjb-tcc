package com.tcc.pjb.backend.service.processual.participacao.workspace;

import java.time.Instant;
import java.util.List;

public record PendingView(String codigo,
                          String titulo,
                          String acaoSugeridaCodigo,
                          String acaoSugeridaLabel,
                          String fase,
                          int prioridade,
                          boolean blocking,
                          Instant dueAt,
                          List<String> tags) {
}
