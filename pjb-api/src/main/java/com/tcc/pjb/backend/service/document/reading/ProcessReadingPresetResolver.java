package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.ui.UsuarioAccessibilityPreference;
import com.tcc.pjb.backend.service.ui.preferences.UiUserPreferenceService;
import com.tcc.pjb.backend.service.ui.presentation.ReadingModeProperties;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessReadingPresetResolver {

    private final UiUserPreferenceService preferenceService;
    private final ReadingModeProperties readingModeProperties;

    public ProcessReadingPresetResolver(UiUserPreferenceService preferenceService,
                                        ReadingModeProperties readingModeProperties) {
        this.preferenceService = Objects.requireNonNull(preferenceService, "preferenceService");
        this.readingModeProperties = Objects.requireNonNull(readingModeProperties, "readingModeProperties");
    }

    public ProcessReadingPresetProfile resolve(Usuario usuario,
                                               Processo processo,
                                               ProcessReadingModeProfile modeProfile) {
        UsuarioAccessibilityPreference preference = usuario != null && usuario.getId() != null
                ? preferenceService.loadOrCreate(usuario.getId())
                : null;
        boolean readingEnabled = preference != null
                ? preference.isReadingModeEnabled()
                : readingModeProperties.isEnabledByDefault();
        UiReadingIntensity intensity = preference != null && preference.getReadingIntensity() != null
                ? preference.getReadingIntensity()
                : defaultIntensity();
        ReadingModeProperties.Intensity cfg = readingModeProperties.resolve(intensity);
        int fontScalePercent = Math.max(cfg.getFontScalePercent(), parseInt(modeProfile.fontScale(), cfg.getFontScalePercent()));
        double lineHeight = Math.max(cfg.getLineHeight(), modeProfile.volumeExtenso() ? 1.78d : 1.68d);
        double paragraphGapRem = Math.max(cfg.getParagraphGapRem(), modeProfile.volumeExtenso() ? 0.95d : 0.78d);
        double letterSpacingEm = Math.max(cfg.getLetterSpacingEm(), modeProfile.volumeExtenso() ? 0.003d : 0.001d);
        int maxWidthCh = Math.min(96, Math.max(58, cfg.getMaxWidthCh() - (modeProfile.volumeExtenso() ? 4 : 0)));
        int chunkPageSize = resolveChunkPageSize(modeProfile, intensity);
        String presetCode = resolvePresetCode(usuario, processo, modeProfile, intensity, readingEnabled);
        String resolvedTheme = resolveTheme(modeProfile, intensity, readingEnabled);
        String focusBandMode = modeProfile.volumeExtenso() ? "FOCO_PROGRESSIVO_POR_BLOCO" : "FOCO_DISCRETO_POR_PECA";
        String privacyVeilMode = modeProfile.sigiloReforcado() ? "MASCARA_SIGILO_E_REDUCAO_DE_EXPOSICAO" : "SEM_MASCARA";
        String keyboardBiasMode = resolveKeyboardBiasMode(usuario);
        String chronologyMode = resolveChronologyMode(processo, modeProfile);
        String citationMode = modeProfile.recursal()
                ? "MAPA_ARTIGOS_PRECEDENTES_E_TEMAS"
                : processo != null && processo.getRamoDireito() == RamoDireito.PENAL
                ? "MAPA_AUTORIA_MATERIALIDADE_E_FUNDAMENTO"
                : "MAPA_ARTIGOS_E_FUNDAMENTOS";
        String operationalOverlayMode = resolveOperationalOverlayMode(usuario, modeProfile);
        String searchAssistMode = modeProfile.coberturaTextualPercentual() >= 65
                ? "BUSCA_SEMANTICA_POR_PECA_E_PAGINA"
                : "BUSCA_SEMANTICA_COM_ALERTA_DE_OCR";
        String anchorMode = modeProfile.recursal() ? "ANCORAS_RECURSAIS_FIXAS" : "ANCORAS_POR_PECA_E_EVENTO";
        return new ProcessReadingPresetProfile(
                readingEnabled,
                intensity.name(),
                presetCode,
                resolvedTheme,
                fontScalePercent,
                lineHeight,
                paragraphGapRem,
                letterSpacingEm,
                maxWidthCh,
                chunkPageSize,
                focusBandMode,
                privacyVeilMode,
                keyboardBiasMode,
                chronologyMode,
                citationMode,
                operationalOverlayMode,
                searchAssistMode,
                anchorMode
        );
    }

    private UiReadingIntensity defaultIntensity() {
        UiReadingIntensity intensity = readingModeProperties.getDefaultIntensity();
        return intensity == null ? UiReadingIntensity.SOFT : intensity;
    }

    private static int resolveChunkPageSize(ProcessReadingModeProfile modeProfile, UiReadingIntensity intensity) {
        int base = switch (intensity) {
            case SOFT -> 12;
            case MEDIUM -> 10;
            case STRONG -> 8;
        };
        if (modeProfile.volumeExtenso()) {
            return Math.max(6, base - 2);
        }
        if (modeProfile.recursal()) {
            return Math.max(7, base - 1);
        }
        return base;
    }

    private static String resolvePresetCode(Usuario usuario,
                                            Processo processo,
                                            ProcessReadingModeProfile modeProfile,
                                            UiReadingIntensity intensity,
                                            boolean readingEnabled) {
        if (!readingEnabled) {
            return "LEITURA_BASE_OPERACIONAL";
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isServidorJudiciario()) {
            return "LEITURA_SERVIDOR_MALHA_OPERACIONAL";
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAssessor()) {
            return modeProfile.recursal() ? "LEITURA_ASSESSORIA_RECURSAL" : "LEITURA_ASSESSORIA_DECISORIA";
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura()) {
            return modeProfile.recursal() ? "LEITURA_MAGISTRATURA_RECURSAL" : "LEITURA_MAGISTRATURA_GABINETE";
        }
        if (processo != null && processo.getRamoDireito() == RamoDireito.PENAL) {
            return intensity == UiReadingIntensity.STRONG ? "LEITURA_PENAL_CRONO_PROBATORIA" : "LEITURA_PENAL_ESTRUTURADA";
        }
        if (modeProfile.volumeExtenso()) {
            return "LEITURA_ACERVO_VOLUMOSO";
        }
        return "LEITURA_JURIDICA_ADAPTATIVA";
    }

    private static String resolveTheme(ProcessReadingModeProfile modeProfile,
                                       UiReadingIntensity intensity,
                                       boolean readingEnabled) {
        if (!readingEnabled) {
            return modeProfile.visualTheme();
        }
        if (modeProfile.sigiloReforcado()) {
            return "AMBAR_RESERVADO";
        }
        return switch (intensity) {
            case SOFT -> modeProfile.visualTheme().toUpperCase(Locale.ROOT).contains("AMBAR") ? modeProfile.visualTheme() : "MARFIM_SUAVE";
            case MEDIUM -> "AMBAR_JURIDICO";
            case STRONG -> "CONTRASTE_REFORCADO";
        };
    }

    private static String resolveKeyboardBiasMode(Usuario usuario) {
        if (usuario != null && usuario.getTipoUsuario() != null && (usuario.getTipoUsuario().isServidorJudiciario() || usuario.getTipoUsuario().isAssessor())) {
            return "ATALHOS_E_FOCO_FIXO";
        }
        return "ATALHOS_PADRAO";
    }

    private static String resolveChronologyMode(Processo processo, ProcessReadingModeProfile modeProfile) {
        if (modeProfile.recursal()) {
            return "LINHA_DO_TEMPO_DECISAO_RECURSO_CONTRARRAZOES";
        }
        if (processo != null && processo.getRamoDireito() == RamoDireito.PENAL) {
            return "LINHA_DO_TEMPO_AUTORIA_MATERIALIDADE_EVENTOS";
        }
        if (modeProfile.volumeExtenso()) {
            return "LINHA_DO_TEMPO_POR_PECA_E_MOVIMENTACAO";
        }
        return "CRONOLOGIA_ASSISTIDA";
    }

    private static String resolveOperationalOverlayMode(Usuario usuario, ProcessReadingModeProfile modeProfile) {
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isServidorJudiciario()) {
            return "PRAZOS_PENDENCIAS_E_IMPULSO";
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAssessor()) {
            return "MINUTA_PRECEDENTE_E_ENFRENTAMENTO";
        }
        if (modeProfile.recursal()) {
            return "RAZOES_CONTRARRAZOES_TEMAS";
        }
        return "MARCADORES_DE_LEITURA";
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
