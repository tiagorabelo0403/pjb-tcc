package com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain;

import java.time.Instant;
import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusIntegracaoInstitucionalExterna;

public record InstitutionalExternalDispatch(
        String dispatchId,
        String jobId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String caixaCodigo,
        DestinatarioInstitucionalKind destinatarioKind,
        PapelProcessualInstitucional papelProcessual,
        CanalComunicacaoInstitucional channel,
        String provider,
        String routingKey,
        String eventType,
        String dedupKey,
        StatusIntegracaoInstitucionalExterna status,
        String providerReference,
        String payloadHash,
        String requestPayload,
        String responsePayload,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public InstitutionalExternalDispatch {
        dispatchId = require(dispatchId, "dispatchId");
        jobId = require(jobId, "jobId");
        expedicaoUuid = require(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(processoId, "processoId");
        unidadeCodigo = require(unidadeCodigo, "unidadeCodigo");
        caixaCodigo = require(caixaCodigo, "caixaCodigo");
        Objects.requireNonNull(destinatarioKind, "destinatarioKind");
        Objects.requireNonNull(papelProcessual, "papelProcessual");
        Objects.requireNonNull(channel, "channel");
        provider = require(provider, "provider");
        routingKey = require(routingKey, "routingKey");
        eventType = require(eventType, "eventType");
        dedupKey = require(dedupKey, "dedupKey");
        Objects.requireNonNull(status, "status");
        payloadHash = require(payloadHash, "payloadHash");
        requestPayload = require(requestPayload, "requestPayload");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public InstitutionalExternalDispatch withAccepted(Instant when, String providerReference, String responsePayload) {
        return new InstitutionalExternalDispatch(
                dispatchId,
                jobId,
                expedicaoUuid,
                processoId,
                processoNumero,
                unidadeCodigo,
                caixaCodigo,
                destinatarioKind,
                papelProcessual,
                channel,
                provider,
                routingKey,
                eventType,
                dedupKey,
                StatusIntegracaoInstitucionalExterna.ACEITA,
                providerReference,
                payloadHash,
                requestPayload,
                responsePayload,
                null,
                createdAt,
                when
        );
    }

    public InstitutionalExternalDispatch withFailure(Instant when, StatusIntegracaoInstitucionalExterna status, String reason, String responsePayload) {
        return new InstitutionalExternalDispatch(
                dispatchId,
                jobId,
                expedicaoUuid,
                processoId,
                processoNumero,
                unidadeCodigo,
                caixaCodigo,
                destinatarioKind,
                papelProcessual,
                channel,
                provider,
                routingKey,
                eventType,
                dedupKey,
                status,
                providerReference,
                payloadHash,
                requestPayload,
                responsePayload,
                reason,
                createdAt,
                when
        );
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " é obrigatório");
        }
        return value.trim();
    }
}
