package com.tcc.pjb.backend.service.secretariat.routing;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfile;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver.SecretariatSpecializationProfile;

public record SecretariatOperationalRoutingProfile(
        String routeKey,
        String tipoJustica,
        String tribunalCodigo,
        String instanciaAxis,
        String regimeAxis,
        String ramoAxis,
        String deskAxis,
        String secretariatCode,
        String receiptQueueCode,
        String receiptInboxKey,
        String saneamentoQueueCode,
        String saneamentoInboxKey,
        String audienceQueueCode,
        String audienceInboxKey,
        String executionQueueCode,
        String executionInboxKey,
        String hearingRoomPrefix,
        String organizationalPath,
        Duration receiptSla,
        Duration saneamentoSla,
        Duration audiencePreparationSla,
        int audienceDefaultDurationMinutes,
        boolean supportsPhysicalRoom,
        boolean supportsVirtualRoom,
        boolean secrecyAware,
        boolean conciliationPreferred,
        List<String> checklist,
        List<String> flags,
        SecretariatSpecializationProfile specialization,
        JudicialScaleProfile scaleProfile,
        Map<String, Object> metadata
) {

    public String getRouteKey() {
        return routeKey();
    }

    public String getTipoJustica() {
        return tipoJustica();
    }

    public String getTribunalCodigo() {
        return tribunalCodigo();
    }

    public String getInstanciaAxis() {
        return instanciaAxis();
    }

    public String getRegimeAxis() {
        return regimeAxis();
    }

    public String getRamoAxis() {
        return ramoAxis();
    }

    public String getDeskAxis() {
        return deskAxis();
    }

    public String getSecretariatCode() {
        return secretariatCode();
    }

    public String getReceiptQueueCode() {
        return receiptQueueCode();
    }

    public String getReceiptInboxKey() {
        return receiptInboxKey();
    }

    public String getSaneamentoQueueCode() {
        return saneamentoQueueCode();
    }

    public String getSaneamentoInboxKey() {
        return saneamentoInboxKey();
    }

    public String getAudienceQueueCode() {
        return audienceQueueCode();
    }

    public String getAudienceInboxKey() {
        return audienceInboxKey();
    }

    public String getExecutionQueueCode() {
        return executionQueueCode();
    }

    public String getExecutionInboxKey() {
        return executionInboxKey();
    }

    public String getHearingRoomPrefix() {
        return hearingRoomPrefix();
    }

    public String getOrganizationalPath() {
        return organizationalPath();
    }

    public Duration getReceiptSla() {
        return receiptSla();
    }

    public Duration getSaneamentoSla() {
        return saneamentoSla();
    }

    public Duration getAudiencePreparationSla() {
        return audiencePreparationSla();
    }

    public Duration getAudienceSla() {
        return audiencePreparationSla();
    }

    public int getAudienceDefaultDurationMinutes() {
        return audienceDefaultDurationMinutes();
    }

    public boolean isSupportsPhysicalRoom() {
        return supportsPhysicalRoom();
    }

    public boolean getSupportsPhysicalRoom() {
        return supportsPhysicalRoom();
    }

    public boolean isSupportsVirtualRoom() {
        return supportsVirtualRoom();
    }

    public boolean getSupportsVirtualRoom() {
        return supportsVirtualRoom();
    }

    public boolean isSecrecyAware() {
        return secrecyAware();
    }

    public boolean getSecrecyAware() {
        return secrecyAware();
    }

    public boolean isConciliationPreferred() {
        return conciliationPreferred();
    }

    public boolean getConciliationPreferred() {
        return conciliationPreferred();
    }

    public List<String> getChecklist() {
        return checklist();
    }

    public List<String> getFlags() {
        return flags();
    }

    public SecretariatSpecializationProfile getSpecialization() {
        return specialization();
    }

    public JudicialScaleProfile getScaleProfile() {
        return scaleProfile();
    }

    public Map<String, Object> getMetadata() {
        return metadata();
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("routeKey", routeKey);
        out.put("tipoJustica", tipoJustica);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("instanciaAxis", instanciaAxis);
        out.put("regimeAxis", regimeAxis);
        out.put("ramoAxis", ramoAxis);
        out.put("deskAxis", deskAxis);
        out.put("secretariatCode", secretariatCode);
        out.put("receiptQueueCode", receiptQueueCode);
        out.put("receiptInboxKey", receiptInboxKey);
        out.put("saneamentoQueueCode", saneamentoQueueCode);
        out.put("saneamentoInboxKey", saneamentoInboxKey);
        out.put("audienceQueueCode", audienceQueueCode);
        out.put("audienceInboxKey", audienceInboxKey);
        out.put("executionQueueCode", executionQueueCode);
        out.put("executionInboxKey", executionInboxKey);
        out.put("hearingRoomPrefix", hearingRoomPrefix);
        out.put("organizationalPath", organizationalPath);
        out.put("receiptSlaHours", receiptSla == null ? null : receiptSla.toHours());
        out.put("saneamentoSlaHours", saneamentoSla == null ? null : saneamentoSla.toHours());
        out.put("audiencePreparationSlaHours", audiencePreparationSla == null ? null : audiencePreparationSla.toHours());
        out.put("audienceDefaultDurationMinutes", audienceDefaultDurationMinutes);
        out.put("supportsPhysicalRoom", supportsPhysicalRoom);
        out.put("supportsVirtualRoom", supportsVirtualRoom);
        out.put("secrecyAware", secrecyAware);
        out.put("conciliationPreferred", conciliationPreferred);
        out.put("checklist", checklist);
        out.put("flags", flags);
        out.put("specialization", specialization == null ? null : specialization.toMap());
        out.put("scaleProfile", scaleProfile == null ? null : scaleProfile.toMap());
        out.put("metadata", metadata);
        out.entrySet().removeIf(entry -> entry.getValue() == null);
        return out;
    }

    public Duration audienceSla() {
        return audiencePreparationSla();
    }
}
