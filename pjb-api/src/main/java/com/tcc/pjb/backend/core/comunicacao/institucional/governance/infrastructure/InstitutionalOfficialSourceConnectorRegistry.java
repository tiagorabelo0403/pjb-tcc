package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalOfficialSourceCatalogService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceCatalogProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorProfile;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class InstitutionalOfficialSourceConnectorRegistry {

    private final InstitutionalOfficialSourceCatalogService catalogService;
    private final InstitutionalOfficialSourceConnectorProperties properties;
    private final InstitutionalOfficialSourceConnectorRuntimeStateRepository runtimeStateRepository;
    private final Clock clock;

    public InstitutionalOfficialSourceConnectorRegistry(InstitutionalOfficialSourceCatalogService catalogService,
                                                        InstitutionalOfficialSourceConnectorProperties properties,
                                                        InstitutionalOfficialSourceConnectorRuntimeStateRepository runtimeStateRepository,
                                                        Clock clock) {
        this.catalogService = Objects.requireNonNull(catalogService);
        this.properties = Objects.requireNonNull(properties);
        this.runtimeStateRepository = Objects.requireNonNull(runtimeStateRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    public InstitutionalOfficialSourceConnectorProfile describe(String sourceCode) {
        InstitutionalOfficialSourceCatalogProfile profile = catalogService.profileFor(sourceCode);
        InstitutionalOfficialSourceConnectorProperties.SourceConfig config = properties.getSources().get(normalize(sourceCode));
        boolean globallyEnabled = properties.isEnabled();
        boolean connectorEnabled = globallyEnabled && (config == null || config.isEnabled());
        Instant checkedAt = Instant.now(clock);
        Instant nextCheckAt = checkedAt.plus(Math.max(1, properties.getRefreshHours()), ChronoUnit.HOURS);
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(profile.defaultFundamentos());
        signals.add("authority=" + profile.authority());
        signals.add("access_mode=" + profile.accessMode());
        if (profile.officialReferenceUrl() != null && !profile.officialReferenceUrl().isBlank()) {
            signals.add("reference_url_present=true");
        }
        String referenceUrl = config != null && config.getReferenceUrl() != null && !config.getReferenceUrl().isBlank()
                ? config.getReferenceUrl().trim()
                : profile.officialReferenceUrl();
        boolean baseUrlPresent = config != null && config.getBaseUrl() != null && !config.getBaseUrl().isBlank();
        boolean dryRun = config == null || config.isDryRun();
        String status;
        boolean liveVerificationSupported;
        if (!connectorEnabled) {
            status = "DESABILITADO";
            liveVerificationSupported = false;
            blockers.add("connector_disabled");
        } else if (!profile.autoRefreshSupported()) {
            status = "HOMOLOGACAO_HUMANA_OBRIGATORIA";
            liveVerificationSupported = false;
            signals.add("manual_review_required=true");
        } else if (baseUrlPresent && !dryRun) {
            status = "PRONTO_PARA_VERIFICACAO_REMOTA";
            liveVerificationSupported = true;
            signals.add("base_url_configured=true");
        } else if (baseUrlPresent) {
            status = "DRY_RUN_PREPARADO";
            liveVerificationSupported = false;
            signals.add("base_url_configured=true");
            blockers.add("connector_still_in_dry_run");
        } else if (profile.directGovernmentSource()) {
            status = "DRY_RUN_PREPARADO";
            liveVerificationSupported = false;
            blockers.add("connector_base_url_pending");
        } else if (referenceUrl != null && !referenceUrl.isBlank()) {
            status = "AGUARDANDO_BASE_URL";
            liveVerificationSupported = false;
            blockers.add("connector_base_url_pending");
        } else {
            status = "NAO_APLICAVEL";
            liveVerificationSupported = false;
            blockers.add("connector_reference_not_available");
        }
        fundamentos.add("connector_status=" + status);
        fundamentos.add("connector_enabled=" + connectorEnabled);
        fundamentos.add("connector_live_verification_supported=" + liveVerificationSupported);
        if (dryRun) {
            fundamentos.add("connector_mode=dry_run");
        }
        if (referenceUrl != null && !referenceUrl.isBlank()) {
            fundamentos.add("connector_reference_url=" + referenceUrl);
        }
        var runtimeSnapshot = runtimeStateRepository.findActive(profile.sourceCode(), checkedAt).orElse(null);
        if (runtimeSnapshot != null) {
            status = runtimeSnapshot.connectorStatus();
            liveVerificationSupported = runtimeSnapshot.liveVerificationSupported();
            checkedAt = runtimeSnapshot.checkedAt();
            nextCheckAt = runtimeSnapshot.nextCheckAt();
            runtimeSnapshot.signals().stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).forEach(signals::add);
            runtimeSnapshot.blockers().stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).forEach(blockers::add);
            runtimeSnapshot.fundamentos().stream().filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).forEach(fundamentos::add);
        }
        return new InstitutionalOfficialSourceConnectorProfile(
                profile.sourceCode(),
                connectorEnabled,
                status,
                liveVerificationSupported,
                referenceUrl,
                checkedAt,
                nextCheckAt,
                List.copyOf(signals),
                List.copyOf(blockers),
                List.copyOf(fundamentos)
        );
    }

    private static String normalize(String sourceCode) {
        return sourceCode == null ? "" : sourceCode.trim().toUpperCase(Locale.ROOT);
    }
}
