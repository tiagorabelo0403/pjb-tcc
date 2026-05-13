package com.tcc.pjb.backend.core.processo.cumprimento.domain;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import java.time.Instant;
import java.util.Objects;

public record ProcessoCumprimentoOperacionalItem(String codigo,
                                                 String titulo,
                                                 String descricao,
                                                 WorkItemType tipo,
                                                 TipoUsuario papelResponsavel,
                                                 String queueCode,
                                                 String inboxKey,
                                                 int prioridade,
                                                 boolean bloqueante,
                                                 Instant dueAt,
                                                 String baseLegal,
                                                 String hashComando) {
    public ProcessoCumprimentoOperacionalItem {
        codigo = Objects.toString(codigo, "").trim();
        titulo = Objects.toString(titulo, "").trim();
        descricao = Objects.toString(descricao, "").trim();
        tipo = tipo == null ? WorkItemType.OUTRO : tipo;
        papelResponsavel = papelResponsavel == null ? TipoUsuario.SERVIDOR_FORUM : papelResponsavel;
        queueCode = Objects.toString(queueCode, "").trim();
        inboxKey = Objects.toString(inboxKey, "").trim();
        baseLegal = Objects.toString(baseLegal, "").trim();
        hashComando = Objects.toString(hashComando, "").trim();
    }
}
