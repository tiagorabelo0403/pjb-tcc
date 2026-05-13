package com.tcc.pjb.backend.service.processual.participacao.workspace;

import java.util.List;

public record RoutingView(String inboxKey,
                          String queueCode,
                          String unidadeRecepcao,
                          String persona,
                          Long actorUserId,
                          List<String> guards) {
}
