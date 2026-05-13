package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeGovernedPetitionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedPetitionRequest;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeGovernedPetitionService {

    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final OfficeGovernedProcessOperationService officeGovernedProcessOperationService;

    public OfficeGovernedPetitionService(OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                         OfficeGovernedProcessOperationService officeGovernedProcessOperationService) {
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.officeGovernedProcessOperationService = Objects.requireNonNull(officeGovernedProcessOperationService);
    }

    @Transactional
    public PjbFrontendOfficeGovernedPetitionView submit(Long processoId,
                                                        FrontendOfficeGovernedPetitionRequest request,
                                                        HttpServletRequest httpServletRequest) {
        PjbFrontendOfficeProcessAccessView access = officeProcessWorkspaceScopeService.access(processoId, OfficeActionType.PETICIONAR, httpServletRequest);
        if (!access.allowed()) {
            throw new IllegalStateException("Peticionamento fora do escopo operacional do workspace: " + String.join(", ", safeList(access.blockers())));
        }
        FrontendOfficeGovernedPetitionRequest safeRequest = request == null
                ? new FrontendOfficeGovernedPetitionRequest("PETICAO_INTERMEDIARIA", "", null)
                : request;
        Map<String, Object> result = officeGovernedProcessOperationService.protocolizarPeticao(
                processoId,
                safeRequest.tipoPeticao(),
                safeRequest.conteudo(),
                safeRequest.fundamentacao()
        );
        return new PjbFrontendOfficeGovernedPetitionView(
                processoId,
                access.mode(),
                access.activeEquipeId(),
                OfficeActionType.PETICIONAR.name(),
                stringValue(result.get("status")),
                longValue(result.get("operationId")),
                longValue(result.get("queueItemId")),
                longValue(result.get("workItemId")),
                stringValue(result.get("dedupKey")),
                access.queueRequired(),
                access.queueRequired() || containsWarning(access.warnings(), "ASSINATURA_PATRONAL_OBRIGATORIA"),
                longValue(result.get("signerUserId")) == null ? access.effectiveSignerUserId() : longValue(result.get("signerUserId")),
                firstNonBlank(stringValue(result.get("signerNome")), access.effectiveSignerNome()),
                stringValue(result.get("signerRegistration")),
                stringValue(result.get("signatureMode")),
                booleanValue(result.get("signatureEnvelopeReady")),
                stringValue(result.get("signedContentHash")),
                stringValue(result.get("signedContent")),
                mapValue(result.get("signatureEnvelope")),
                safeList(access.blockers()),
                mergeWarnings(access.warnings(), safeStringList(result.get("warnings")))
        );
    }

    private List<String> mergeWarnings(List<String> left, List<String> right) {
        LinkedHashMap<String, String> ordered = new LinkedHashMap<>();
        for (String item : safeList(left)) {
            ordered.put(item, item);
        }
        for (String item : safeList(right)) {
            ordered.put(item, item);
        }
        return List.copyOf(ordered.values());
    }

    private boolean containsWarning(List<String> warnings, String value) {
        return safeList(warnings).stream().anyMatch(value::equalsIgnoreCase);
    }

    private List<String> safeStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(Object::toString).toList();
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        map.forEach((k, v) -> out.put(String.valueOf(k), v));
        return out;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private boolean booleanValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
