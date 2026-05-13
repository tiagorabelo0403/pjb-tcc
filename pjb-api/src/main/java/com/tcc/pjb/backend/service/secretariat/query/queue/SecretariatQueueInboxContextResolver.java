package com.tcc.pjb.backend.service.secretariat.query.queue;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskKey;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioResolver;
import com.tcc.pjb.backend.core.forum.routing.ForumInstance;
import com.tcc.pjb.backend.core.forum.routing.ForumLane;
import com.tcc.pjb.backend.core.forum.routing.JudicialOrganKind;
import com.tcc.pjb.backend.core.forum.routing.JudicialOrganRef;
import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatDeskLoadProfile;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatDeskLoadResolver;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SecretariatQueueInboxContextResolver {

    private final SecretariatInstitutionalVisibilityService visibilityService;
    private final ForumDeskPortfolioResolver portfolioResolver;
    private final SecretariatQueueLoadResolver loadResolver;
    private final SecretariatDeskLoadResolver deskLoadResolver;

    public SecretariatQueueInboxContextResolver(SecretariatInstitutionalVisibilityService visibilityService,
                                                ForumDeskPortfolioResolver portfolioResolver,
                                                SecretariatQueueLoadResolver loadResolver,
                                                SecretariatDeskLoadResolver deskLoadResolver) {
        this.visibilityService = Objects.requireNonNull(visibilityService);
        this.portfolioResolver = Objects.requireNonNull(portfolioResolver);
        this.loadResolver = Objects.requireNonNull(loadResolver);
        this.deskLoadResolver = Objects.requireNonNull(deskLoadResolver);
    }

    public SecretariatQueueInboxContext resolve(String inboxKey, Collection<String> statuses) {
        String authorizedInboxKey = requireAuthorizedInbox(inboxKey);
        List<String> effectiveStatuses = normalizeStatuses(statuses);
        SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile inboxProfile = visibilityService.describeAuthorizedInbox(authorizedInboxKey);
        ForumDeskPortfolioProfile portfolio = resolvePortfolio(authorizedInboxKey);
        SecretariatQueueLoadProfile loadProfile = loadResolver.resolve(authorizedInboxKey, effectiveStatuses);
        SecretariatDeskLoadProfile deskProfile = deskLoadResolver.resolve(authorizedInboxKey, effectiveStatuses, portfolio);
        return new SecretariatQueueInboxContext(
            authorizedInboxKey,
            effectiveStatuses,
            inboxProfile,
            portfolio,
            loadProfile,
            deskProfile,
            resolveInboxDescriptor(authorizedInboxKey, portfolio),
            portfolio.dashboardBucket()
        );
    }

    public String requireAuthorizedInbox(String inboxKey) {
        String authorized = visibilityService.requireInboxAccess(inboxKey);
        return firstNonBlank(authorized, inboxKey, "SEC:TJCE:FORTALEZA:COMUM:CE:morada-nova:1a-vara");
    }

    public List<String> normalizeStatuses(Collection<String> statuses) {
        return statuses == null || statuses.isEmpty() ? List.of("PENDENTE", "EM_EXECUCAO") : List.copyOf(statuses);
    }

    private ForumDeskPortfolioProfile resolvePortfolio(String inboxKey) {
        if (inboxKey == null || inboxKey.isBlank()) {
            return fallbackPortfolio(inboxKey);
        }
        SecretariatInboxKeyParser.Parts parsed = SecretariatInboxKeyParser.parse(inboxKey).orElse(null);
        if (parsed == null) {
            return fallbackPortfolio(inboxKey);
        }
        JudicialOrganKind kind = organKind(parsed.org());
        JudicialOrganRef organ = new JudicialOrganRef(normalizeToken(parsed.org()) == null ? "UNKNOWN" : normalizeToken(parsed.org()), kind, parsed.org());
        ForumDeskKey deskKey = new ForumDeskKey(
                parsed.normalized(),
                organ,
                instance(parsed.instance()),
                ForumLane.fromToken(parsed.lane()).orElse(ForumLane.COMUM),
                normalizeToken(parsed.uf()),
                normalizeToken(parsed.comarca()),
                normalizeToken(parsed.jurisdicao())
        );
        return portfolioResolver.resolve(deskKey);
    }

    private String resolveInboxDescriptor(String inboxKey, ForumDeskPortfolioProfile portfolio) {
        return portfolio == null ? inboxKey : portfolio.operationalDescriptor();
    }

    private static String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static ForumInstance instance(String token) {
        if (token == null || token.isBlank()) {
            return ForumInstance.FIRST;
        }
        return switch (token.trim().toUpperCase(Locale.ROOT)) {
            case "PRIMEIRA_INSTANCIA", "1G", "1" -> ForumInstance.FIRST;
            case "SEGUNDA_INSTANCIA", "2G", "2" -> ForumInstance.SECOND;
            case "SUPERIOR", "3G", "TRIBUNAL_SUPERIOR", "3" -> ForumInstance.SUPERIOR;
            default -> ForumInstance.FIRST;
        };
    }

    private static JudicialOrganKind organKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return JudicialOrganKind.UNKNOWN;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "VARA", "UNIDADE_JUDICIAL" -> JudicialOrganKind.UNKNOWN;
            case "TJ", "TURMA" -> JudicialOrganKind.TJ;
            case "TRF" -> JudicialOrganKind.TRF;
            case "TRT" -> JudicialOrganKind.TRT;
            case "TRE" -> JudicialOrganKind.TRE;
            case "TJM" -> JudicialOrganKind.TJM;
            case "STM" -> JudicialOrganKind.STM;
            case "STJ" -> JudicialOrganKind.STJ;
            case "STF" -> JudicialOrganKind.STF;
            case "TST" -> JudicialOrganKind.TST;
            case "TSE" -> JudicialOrganKind.TSE;
            case "CAMARA", "CÂMARA" -> JudicialOrganKind.TJ;
            case "GABINETE" -> JudicialOrganKind.UNKNOWN;
            case "NUCLEO", "NÚCLEO" -> JudicialOrganKind.UNKNOWN;
            case "SECRETARIA" -> JudicialOrganKind.UNKNOWN;
            case "COLEGIADO" -> JudicialOrganKind.UNKNOWN;
            default -> JudicialOrganKind.UNKNOWN;
        };
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

    private ForumDeskPortfolioProfile fallbackPortfolio(String inboxKey) {
        return new ForumDeskPortfolioProfile(
                "TRIAGE",
                "GABINETE",
                "HEARING",
                "COMPLIANCE",
                "ESCALATION",
                "ASSISTANT",
                "COORDINATION",
                "REDISTRIBUTION",
                normalizeToken(inboxKey),
                List.of(),
                new java.util.LinkedHashMap<>()
        );
    }
}
