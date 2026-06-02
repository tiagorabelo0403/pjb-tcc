package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingPreferenceResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingPresetCatalogResponse;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

final class ProcessReadingPresetCatalogResolver {

    ProcessReadingPresetCatalogResponse resolve(Long processoId,
                                                ProcessReadingWorkspaceSession session) {
        ProcessReadingPreferenceResponse activePreference = toPreferenceResponse(session.presetProfile());
        LinkedHashSet<String> availablePresets = resolveAvailablePresets(session.usuario(), session.context().processo(), session.modeProfile(), session.presetProfile());
        LinkedHashMap<String, Boolean> featureFlags = new LinkedHashMap<>();
        featureFlags.put("supportsInstitutionalPreset", true);
        featureFlags.put("supportsAmberMode", true);
        featureFlags.put("supportsPrivacyVeil", session.modeProfile().sigiloReforcado() || !"SEM_MASCARA".equals(session.presetProfile().privacyVeilMode()));
        featureFlags.put("supportsKeyboardBias", true);
        featureFlags.put("supportsChronology", true);
        featureFlags.put("supportsCitationMap", true);
        featureFlags.put("supportsOperationalOverlay", true);
        featureFlags.put("supportsNativeActs", !session.context().movimentacoes().isEmpty() || !session.context().eventos().isEmpty());
        featureFlags.put("supportsInlineDecisions", true);
        featureFlags.put("supportsProceduralContextMesh", true);
        featureFlags.put("supportsReadingSpecialization", true);
        LinkedHashMap<String, Object> frontend = new LinkedHashMap<>();
        frontend.put("preferenceEndpoint", "/api/v1/ui/accessibility/preference");
        frontend.put("catalogEndpoint", "/api/v1/processos/" + processoId + "/painel-leitura/presets");
        frontend.put("readerEndpoint", "/api/v1/processos/" + processoId + "/painel-leitura");
        frontend.put("flowEndpoint", "/api/v1/processos/" + processoId + "/painel-leitura/fluxo");
        frontend.put("contentEndpoint", "/api/v1/processos/" + processoId + "/painel-leitura/conteudo");
        frontend.put("contextEndpoint", "/api/v1/processos/" + processoId + "/painel-leitura/contexto-procedimental");
        frontend.put("specializationEndpoint", "/api/v1/processos/" + processoId + "/painel-leitura/especializacao");
        frontend.put("documentContentEndpointTemplate", "/api/v1/documentos/{documentoId}/painel-leitura/conteudo");
        frontend.put("availableFontScaleOptions", List.of("92", "100", "108", "112", "120", "128"));
        frontend.put("availableLineSpacingOptions", List.of("COMPACTO", "PADRAO_LIMPO", "EXPANDIDO"));
        return new ProcessReadingPresetCatalogResponse(
                activePreference,
                List.of("AMBAR_JURIDICO", "AMBAR_RESERVADO", "MARFIM_SUAVE", "NEUTRO_ESTATUARIO", "CONTRASTE_REFORCADO"),
                List.of(UiReadingIntensity.SOFT.name(), UiReadingIntensity.MEDIUM.name(), UiReadingIntensity.STRONG.name()),
                List.copyOf(availablePresets),
                List.of("FOCO_DISCRETO_POR_PECA", "FOCO_PROGRESSIVO_POR_BLOCO"),
                List.of("MARCADORES_DE_LEITURA", "PRAZOS_PENDENCIAS_E_IMPULSO", "MINUTA_PRECEDENTE_E_ENFRENTAMENTO", "RAZOES_CONTRARRAZOES_TEMAS"),
                featureFlags,
                frontend
        );
    }

    static ProcessReadingPreferenceResponse toPreferenceResponse(ProcessReadingPresetProfile presetProfile) {
        return new ProcessReadingPreferenceResponse(
                presetProfile.readingModeEnabled(),
                presetProfile.intensity(),
                presetProfile.presetCode(),
                presetProfile.resolvedTheme(),
                presetProfile.fontScalePercent(),
                presetProfile.lineHeight(),
                presetProfile.paragraphGapRem(),
                presetProfile.letterSpacingEm(),
                presetProfile.maxWidthCh(),
                presetProfile.chunkPageSize(),
                presetProfile.focusBandMode(),
                presetProfile.privacyVeilMode(),
                presetProfile.keyboardBiasMode(),
                presetProfile.chronologyMode(),
                presetProfile.citationMode(),
                presetProfile.operationalOverlayMode(),
                presetProfile.searchAssistMode(),
                presetProfile.anchorMode()
        );
    }

    private LinkedHashSet<String> resolveAvailablePresets(Usuario usuario,
                                                          Processo processo,
                                                          ProcessReadingModeProfile modeProfile,
                                                          ProcessReadingPresetProfile presetProfile) {
        LinkedHashSet<String> presets = new LinkedHashSet<>();
        presets.add(presetProfile.presetCode());
        presets.add("LEITURA_JURIDICA_ADAPTATIVA");
        if (modeProfile.volumeExtenso()) {
            presets.add("LEITURA_ACERVO_VOLUMOSO");
        }
        if (modeProfile.recursal()) {
            presets.add(usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura()
                    ? "LEITURA_MAGISTRATURA_RECURSAL"
                    : "LEITURA_ASSESSORIA_RECURSAL");
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isServidorJudiciario()) {
            presets.add("LEITURA_SERVIDOR_MALHA_OPERACIONAL");
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAssessor()) {
            presets.add("LEITURA_ASSESSORIA_DECISORIA");
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura()) {
            presets.add("LEITURA_MAGISTRATURA_GABINETE");
        }
        if (processo != null && processo.getRamoDireito() != null && processo.getRamoDireito().name().contains("PENAL")) {
            presets.add("LEITURA_PENAL_ESTRUTURADA");
            presets.add("LEITURA_PENAL_CRONO_PROBATORIA");
        }
        return presets;
    }

    static String formatLineSpacing(double lineHeight) {
        return String.format(Locale.ROOT, "%.2f", lineHeight);
    }
}
