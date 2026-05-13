package com.tcc.pjb.backend.service.secretariat.query.routing;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SecretariatHearingMediaLaneService {

    public HearingMediaLaneSnapshot resolve(String inboxKey,
                                            String queueCode,
                                            String title,
                                            Collection<String> tags,
                                            ForumDeskPortfolioProfile portfolio,
                                            SecretariatFlowBridgeProfile bridgeProfile,
                                            SecretariatJudicialIntegrationProfile integrationProfile) {
        String source = normalize(((title == null ? "" : title) + ' ' + (queueCode == null ? "" : queueCode) + ' ' + (tags == null ? "" : String.join(" ", tags))));
        boolean hearingRelated = containsAny(source, "AUDIENCIA", "AUDIÊNCIA", "WEBRTC", "DEPOIMENTO", "ATA", "TESTEMUNHA");
        boolean sessionRelated = containsAny(source, "SESSAO", "SESSÃO", "SUSTENTACAO", "SUSTENTAÇÃO", "PAUTA", "ACORDAO", "ACÓRDÃO");
        boolean mediaRelated = containsAny(source, "VIDEO", "VÍDEO", "AUDIO", "ÁUDIO", "MIDIA", "MÍDIA", "GRAVACAO", "GRAVAÇÃO", "TRANSCRICAO", "TRANSCRIÇÃO");
        boolean labourMedia = containsAny(source, "TRABALHISTA", "GRU", "PJE_MIDIAS", "MÍDIAS", "ACERVO_DIGITAL")
                || integrationProfile != null && "TRT".equals(integrationProfile.tribunalCodigo());
        String journeyMode = bridgeProfile == null ? "FIRST_INSTANCE_SECRETARIAT" : firstNonBlank(bridgeProfile.bridgeMode(), "FIRST_INSTANCE_SECRETARIAT");
        String targetDesk = labourMedia
                ? firstNonBlank(portfolio == null ? null : portfolio.hearingDesk(), "MESA_MIDIAS_PROCESSUAIS")
                : sessionRelated
                ? "MESA_SESSAO_COLEGIADA"
                : hearingRelated
                ? firstNonBlank(portfolio == null ? null : portfolio.hearingDesk(), "MESA_AUDIENCIA")
                : firstNonBlank(portfolio == null ? null : portfolio.assistantDesk(), "MESA_OPERACIONAL");
        String indexingMode = mediaRelated && (hearingRelated || sessionRelated)
                ? "TRANSCRICAO_ANCORAS_EVENTOS"
                : mediaRelated
                ? "INDEXACAO_MIDIA_BASICA"
                : "SEM_INDEXACAO_ESPECIAL";
        String agendaReflection = hearingRelated || sessionRelated
                ? "AGENDA_FILA_MESA"
                : "FILA_ONLY";
        String connectorMediaDecision = mediaRelated && integrationProfile != null && integrationProfile.connectorSystem() != null
                ? "SINCRONIZAR_MARCADORES_COM_CONECTOR"
                : mediaRelated
                ? "INDEXAR_NO_PJB_E_PUBLICAR_REFERENCIAS"
                : "SEM_ACAO_DE_MIDIA";

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        if (hearingRelated) {
            labels.add("HEARING_RELATED");
        }
        if (sessionRelated) {
            labels.add("SESSION_RELATED");
        }
        if (mediaRelated) {
            labels.add("MEDIA_RELATED");
        }
        labels.add(indexingMode);
        labels.add(connectorMediaDecision);
        labels.add(agendaReflection);
        labels.add(journeyMode);

        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("inboxKey", inboxKey);
        diagnostics.put("queueCode", queueCode);
        diagnostics.put("targetDesk", targetDesk);
        diagnostics.put("indexingMode", indexingMode);
        diagnostics.put("agendaReflection", agendaReflection);
        diagnostics.put("connectorMediaDecision", connectorMediaDecision);
        diagnostics.put("labourMedia", labourMedia);

        return new HearingMediaLaneSnapshot(
                hearingRelated,
                sessionRelated,
                mediaRelated,
                targetDesk,
                indexingMode,
                agendaReflection,
                connectorMediaDecision,
                List.copyOf(labels),
                java.util.Map.copyOf(diagnostics)
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean containsAny(String source, String... tokens) {
        if (source == null || source.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && source.contains(token.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public record HearingMediaLaneSnapshot(boolean hearingRelated,
                                           boolean sessionRelated,
                                           boolean mediaRelated,
                                           String targetDesk,
                                           String indexingMode,
                                           String agendaReflection,
                                           String connectorMediaDecision,
                                           List<String> labels,
                                           java.util.Map<String, Object> diagnostics) {
    }
}
