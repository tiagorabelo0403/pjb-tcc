package com.tcc.pjb.backend.service.secretariat.query.routing;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;

@Component
public class SecretariatFlowBridgeResolver {

    public SecretariatFlowBridgeProfile resolve(String inboxKey,
                                                String queueCode,
                                                String title,
                                                Collection<String> tags,
                                                ForumDeskPortfolioProfile portfolio) {
        String source = ((inboxKey == null ? "" : inboxKey) + ' '
                + (queueCode == null ? "" : queueCode) + ' '
                + (title == null ? "" : title) + ' '
                + (tags == null ? "" : String.join(" ", tags)))
                .toUpperCase(Locale.ROOT);

        boolean recursal = containsAny(source, "RECURSO", "APEL", "AGRAVO", "RESP", "EXTRAORD", "CONTRARRAZ", "ADMISS");
        boolean gabinete = containsAny(source, "DESPACHO", "DECISAO", "SENTENCA", "CONCLUSO", "GABINETE", "MINUTA");
        boolean distribuicao = containsAny(source, "DISTRIB", "AUTUACAO", "PREVEN", "DEPEND", "PROTOCOLO", "AJUIZ");

        String downstreamAxis = recursal ? "RECURSAL" : gabinete ? "GABINETE" : distribuicao ? "DISTRIBUICAO" : "SECRETARIA";
        String bridgeMode = recursal && gabinete ? "SECRETARIA_GABINETE_RECURSAL"
                : recursal ? "SECRETARIA_RECURSAL"
                : gabinete ? "SECRETARIA_GABINETE"
                : distribuicao ? "SECRETARIA_DISTRIBUICAO"
                : "RETENCAO_LOCAL";

        String distributionDesk = (distribuicao || recursal) && portfolio != null ? portfolio.redistributionDesk() : null;
        String gabineteDesk = (gabinete || recursal) && portfolio != null ? portfolio.gabineteDesk() : null;
        String recursalDesk = recursal ? recursalDeskName(source, portfolio) : null;
        String admissibilityDesk = recursal && containsAny(source, "RESP", "RE", "ADMISS", "ESPECIAL", "EXTRAORD")
                ? "ADMISSIBILIDADE_RECURSAL"
                : null;

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(downstreamAxis);
        labels.add(bridgeMode);
        if (recursal) {
            labels.add("SYNC_RECURSAL");
        }
        if (gabinete) {
            labels.add("SYNC_GABINETE");
        }
        if (distribuicao) {
            labels.add("SYNC_DISTRIBUICAO");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("inboxKey", inboxKey);
        metadata.put("queueCode", queueCode);
        metadata.put("title", title);
        metadata.put("triageDesk", portfolio == null ? null : portfolio.triageDesk());
        metadata.put("coordinationDesk", portfolio == null ? null : portfolio.coordinationDesk());
        metadata.put("assistantDesk", portfolio == null ? null : portfolio.assistantDesk());
        metadata.put("hearingDesk", portfolio == null ? null : portfolio.hearingDesk());
        metadata.put("descriptor", downstreamAxis + ':' + bridgeMode + ':' + (recursalDesk == null ? "LOCAL" : recursalDesk));

        return new SecretariatFlowBridgeProfile(
                downstreamAxis,
                bridgeMode,
                distributionDesk,
                gabineteDesk,
                recursalDesk,
                admissibilityDesk,
                distribuicao || recursal,
                gabinete || recursal,
                recursal,
                List.copyOf(labels),
                metadata
        );
    }

    private static String recursalDeskName(String source, ForumDeskPortfolioProfile portfolio) {
        if (containsAny(source, "RESP", "RE", "ESPECIAL", "EXTRAORD", "ADMISS")) {
            return "SECRETARIA_ADMISSIBILIDADE";
        }
        if (portfolio != null && portfolio.dashboardBucket() != null && portfolio.dashboardBucket().contains("SECOND")) {
            return "SECRETARIA_RECURSAL_COLEGIADA";
        }
        return "SECRETARIA_RECURSAL";
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
}
