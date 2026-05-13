package com.tcc.pjb.backend.core.procedural.atermacao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;

public final class PjbAtermacaoGuidedIntakeService {

    public PjbAtermacaoGuidedIntakePlan prepare(PjbAtermacaoGuidedIntakeRequest request) {
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        LinkedHashSet<String> steps = new LinkedHashSet<>();
        if (request == null || Objects.toString(request.narrative(), "").trim().length() < 40) {
            missing.add("NARRATIVA_MINIMA_DOS_FATOS");
        }
        if (request == null || Objects.toString(request.requestedRelief(), "").isBlank()) {
            missing.add("PEDIDO_PRINCIPAL");
        }
        if (request == null || request.documents() == null || request.documents().isEmpty()) {
            missing.add("DOCUMENTOS_BASE");
        }
        if (request == null || request.estimatedValue() == null || request.estimatedValue().compareTo(BigDecimal.ZERO) < 0) {
            missing.add("VALOR_DA_CAUSA_ESTIMADO");
        }
        if (request != null && request.urgent()) {
            steps.add("encaminhar para triagem humana de tutela de urgência");
        }
        if (request != null && request.vulnerableParty()) {
            steps.add("oferecer linguagem simples e encaminhamento assistido");
        }
        if (request != null && request.publicEntityDefendant()) {
            steps.add("validar competência fazendária ou federal antes da distribuição");
        }
        if (steps.isEmpty()) {
            steps.add("gerar minuta estruturada para revisão de servidor ou defensor");
        }
        PjbAtermacaoRiskLevel risk = risk(request, missing.size());
        String status = missing.isEmpty() ? "READY_FOR_ASSISTED_REVIEW" : "INCOMPLETE_INTAKE";
        return new PjbAtermacaoGuidedIntakePlan(status, risk, missing.size() <= 1, new ArrayList<>(missing), new ArrayList<>(steps));
    }

    private PjbAtermacaoRiskLevel risk(PjbAtermacaoGuidedIntakeRequest request, int missing) {
        if (missing >= 3) {
            return PjbAtermacaoRiskLevel.HUMAN_REVIEW_REQUIRED;
        }
        if (request != null && (request.urgent() || request.vulnerableParty())) {
            return PjbAtermacaoRiskLevel.HIGH;
        }
        if (request != null && request.publicEntityDefendant()) {
            return PjbAtermacaoRiskLevel.MEDIUM;
        }
        return missing == 0 ? PjbAtermacaoRiskLevel.LOW : PjbAtermacaoRiskLevel.MEDIUM;
    }
}
