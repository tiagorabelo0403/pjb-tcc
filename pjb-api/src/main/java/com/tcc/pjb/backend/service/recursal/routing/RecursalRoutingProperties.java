package com.tcc.pjb.backend.service.recursal.routing;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@ConfigurationProperties(prefix = "pjb.recursal.routing")
public class RecursalRoutingProperties {

    private int defaultCabinetSlots = 32;
    private Map<String, Integer> cabinetSlotsByCourt = new HashMap<>();
    private Map<String, Integer> cabinetSlotsByInstance = new HashMap<>();
    private Map<String, Integer> priorityByLane = new HashMap<>();
    private Map<String, Integer> dueDaysByLane = new HashMap<>();
    private boolean emitOriginSecretaryWorkItemOnAppealFiled = true;
    private boolean emitTargetTriageWorkItemOnAppealFiled = true;
    private int defaultSecretariatDueDays = 5;
    private int defaultMagistrateDueDays = 3;
    private int defaultUrgentDueDays = 1;
    private int defaultUpperCourtDueDays = 2;
    private int defaultSecretariatPriority = 3;
    private int defaultMagistratePriority = 2;
    private int defaultUrgentPriority = 1;
    private int defaultRestrictedPriority = 2;

    public int getDefaultCabinetSlots() {
        return defaultCabinetSlots;
    }

    public void setDefaultCabinetSlots(int defaultCabinetSlots) {
        this.defaultCabinetSlots = Math.max(1, defaultCabinetSlots);
    }

    public Map<String, Integer> getCabinetSlotsByCourt() {
        return cabinetSlotsByCourt;
    }

    public void setCabinetSlotsByCourt(Map<String, Integer> cabinetSlotsByCourt) {
        this.cabinetSlotsByCourt = sanitizeNumericMap(cabinetSlotsByCourt);
    }

    public Map<String, Integer> getCabinetSlotsByInstance() {
        return cabinetSlotsByInstance;
    }

    public void setCabinetSlotsByInstance(Map<String, Integer> cabinetSlotsByInstance) {
        this.cabinetSlotsByInstance = sanitizeNumericMap(cabinetSlotsByInstance);
    }

    public Map<String, Integer> getPriorityByLane() {
        return priorityByLane;
    }

    public void setPriorityByLane(Map<String, Integer> priorityByLane) {
        this.priorityByLane = sanitizeNumericMap(priorityByLane);
    }

    public Map<String, Integer> getDueDaysByLane() {
        return dueDaysByLane;
    }

    public void setDueDaysByLane(Map<String, Integer> dueDaysByLane) {
        this.dueDaysByLane = sanitizeNumericMap(dueDaysByLane);
    }

    public boolean isEmitOriginSecretaryWorkItemOnAppealFiled() {
        return emitOriginSecretaryWorkItemOnAppealFiled;
    }

    public void setEmitOriginSecretaryWorkItemOnAppealFiled(boolean emitOriginSecretaryWorkItemOnAppealFiled) {
        this.emitOriginSecretaryWorkItemOnAppealFiled = emitOriginSecretaryWorkItemOnAppealFiled;
    }

    public boolean isEmitTargetTriageWorkItemOnAppealFiled() {
        return emitTargetTriageWorkItemOnAppealFiled;
    }

    public void setEmitTargetTriageWorkItemOnAppealFiled(boolean emitTargetTriageWorkItemOnAppealFiled) {
        this.emitTargetTriageWorkItemOnAppealFiled = emitTargetTriageWorkItemOnAppealFiled;
    }

    public int getDefaultSecretariatDueDays() {
        return defaultSecretariatDueDays;
    }

    public void setDefaultSecretariatDueDays(int defaultSecretariatDueDays) {
        this.defaultSecretariatDueDays = Math.max(1, defaultSecretariatDueDays);
    }

    public int getDefaultMagistrateDueDays() {
        return defaultMagistrateDueDays;
    }

    public void setDefaultMagistrateDueDays(int defaultMagistrateDueDays) {
        this.defaultMagistrateDueDays = Math.max(1, defaultMagistrateDueDays);
    }

    public int getDefaultUrgentDueDays() {
        return defaultUrgentDueDays;
    }

    public void setDefaultUrgentDueDays(int defaultUrgentDueDays) {
        this.defaultUrgentDueDays = Math.max(0, defaultUrgentDueDays);
    }

    public int getDefaultUpperCourtDueDays() {
        return defaultUpperCourtDueDays;
    }

    public void setDefaultUpperCourtDueDays(int defaultUpperCourtDueDays) {
        this.defaultUpperCourtDueDays = Math.max(1, defaultUpperCourtDueDays);
    }

    public int getDefaultSecretariatPriority() {
        return defaultSecretariatPriority;
    }

    public void setDefaultSecretariatPriority(int defaultSecretariatPriority) {
        this.defaultSecretariatPriority = clampPriority(defaultSecretariatPriority);
    }

    public int getDefaultMagistratePriority() {
        return defaultMagistratePriority;
    }

    public void setDefaultMagistratePriority(int defaultMagistratePriority) {
        this.defaultMagistratePriority = clampPriority(defaultMagistratePriority);
    }

    public int getDefaultUrgentPriority() {
        return defaultUrgentPriority;
    }

    public void setDefaultUrgentPriority(int defaultUrgentPriority) {
        this.defaultUrgentPriority = clampPriority(defaultUrgentPriority);
    }

    public int getDefaultRestrictedPriority() {
        return defaultRestrictedPriority;
    }

    public void setDefaultRestrictedPriority(int defaultRestrictedPriority) {
        this.defaultRestrictedPriority = clampPriority(defaultRestrictedPriority);
    }

    public int resolveCabinetSlots(String court, String instanceTag) {
        String normalizedCourt = normalize(court);
        String normalizedInstance = normalize(instanceTag);
        Integer courtSpecific = normalizedCourt == null ? null : cabinetSlotsByCourt.get(normalizedCourt);
        if (courtSpecific != null && courtSpecific > 0) {
            return courtSpecific;
        }
        Integer instanceSpecific = normalizedInstance == null ? null : cabinetSlotsByInstance.get(normalizedInstance);
        if (instanceSpecific != null && instanceSpecific > 0) {
            return instanceSpecific;
        }
        return defaultCabinetSlots;
    }

    public int resolvePriority(String lane,
                               Integer explicitPriority,
                               boolean urgent,
                               boolean blocking,
                               boolean restricted,
                               boolean upperCourt) {
        if (explicitPriority != null) {
            return clampPriority(explicitPriority);
        }
        if (urgent) {
            return defaultUrgentPriority;
        }
        Integer lanePriority = lane == null ? null : priorityByLane.get(normalize(lane));
        if (lanePriority != null) {
            return clampPriority(lanePriority);
        }
        if (restricted) {
            return defaultRestrictedPriority;
        }
        if (upperCourt || blocking) {
            return Math.min(defaultMagistratePriority, defaultRestrictedPriority);
        }
        return defaultSecretariatPriority;
    }

    public int resolveDueDays(String lane, boolean urgent, boolean blocking, boolean upperCourt) {
        if (urgent) {
            return defaultUrgentDueDays;
        }
        Integer laneDueDays = lane == null ? null : dueDaysByLane.get(normalize(lane));
        if (laneDueDays != null) {
            return Math.max(0, laneDueDays);
        }
        if (upperCourt) {
            return defaultUpperCourtDueDays;
        }
        if (blocking) {
            return defaultMagistrateDueDays;
        }
        return defaultSecretariatDueDays;
    }

    public String resolveOriginSecretaryLane(RitoProcessual rito, LegalAppealType appeal) {
        if (appeal == LegalAppealType.RESP || appeal == LegalAppealType.RE || appeal == LegalAppealType.AGRAVO_RESP_RE) {
            return "SECRETARIA_ADMISSIBILIDADE";
        }
        if (rito == null) {
            return "SECRETARIA_RECURSAL";
        }
        if (rito.isEleitoral()) {
            return "SECRETARIA_ELEITORAL";
        }
        if (rito.isMilitar()) {
            return "SECRETARIA_MILITAR";
        }
        if (rito.isTrabalhista()) {
            return "SECRETARIA_TRAB";
        }
        if (rito.isPrevidenciario()) {
            return "SECRETARIA_PREVID";
        }
        if (rito.isTribFazenda()) {
            return "SECRETARIA_FAZENDA";
        }
        if (rito.isPenal()) {
            return "SECRETARIA_PENAL";
        }
        if (rito.name().contains("JUIZADO")) {
            return "SECRETARIA_JEC";
        }
        return "SECRETARIA_RECURSAL";
    }

    private static Map<String, Integer> sanitizeNumericMap(Map<String, Integer> raw) {
        Map<String, Integer> sanitized = new HashMap<>();
        if (raw == null) {
            return sanitized;
        }
        raw.forEach((key, value) -> {
            String normalizedKey = normalize(key);
            if (normalizedKey != null && value != null && value > 0) {
                sanitized.put(normalizedKey, value);
            }
        });
        return sanitized;
    }

    private static int clampPriority(int raw) {
        return Math.max(1, Math.min(5, raw));
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return normalized.isBlank() ? null : normalized;
    }
}
