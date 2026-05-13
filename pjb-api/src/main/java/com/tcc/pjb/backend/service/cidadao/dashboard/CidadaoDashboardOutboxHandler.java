package com.tcc.pjb.backend.service.cidadao.dashboard;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;

@Component
public class CidadaoDashboardOutboxHandler {

    private final CidadaoDashboardSnapshotWriteService writer;
    private final ObjectMapper mapper;

    public CidadaoDashboardOutboxHandler(CidadaoDashboardSnapshotWriteService writer, ObjectMapper mapper) {
        this.writer = Objects.requireNonNull(writer);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public void dispatch(OutboxEvent e) {
        if (e == null || e.getPayloadJson() == null || e.getPayloadJson().isBlank()) return;
        try {
            CidadaoDashboardRefreshRequest req = mapper.readValue(e.getPayloadJson(), CidadaoDashboardRefreshRequest.class);
            if (req == null || req.cpf() == null || req.cpf().isBlank()) return;
            writer.refreshForCpf(req.cpf());
        } catch (Exception ignored) {
        }
    }
}
