package com.tcc.pjb.backend.service.juiz.routing;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.topology.NationalJudicialTopologyService;
import com.tcc.pjb.backend.service.secretariat.topology.NationalJudicialTopologyService.NationalJudicialTopologyProfile;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuizGabineteRoutingResolver {

    private final SecretariatOperationalRoutingResolver secretariatRoutingResolver;
    private final NationalJudicialTopologyService nationalJudicialTopologyService;

    public JuizGabineteRoutingResolver(SecretariatOperationalRoutingResolver secretariatRoutingResolver,
                                       NationalJudicialTopologyService nationalJudicialTopologyService) {
        this.secretariatRoutingResolver = Objects.requireNonNull(secretariatRoutingResolver);
        this.nationalJudicialTopologyService = Objects.requireNonNull(nationalJudicialTopologyService);
    }

    public JuizGabineteRoutingProfile resolve(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        SecretariatOperationalRoutingProfile secretariatRouting = secretariatRoutingResolver.resolve(processo);
        TipoJustica tipoJustica = TipoJustica.fromString(secretariatRouting.tipoJustica());
        NationalJudicialTopologyProfile topology = nationalJudicialTopologyService.resolveForProcess(processo, tipoJustica);
        String gabineteDesk = firstNonBlank(topology.gabineteDesk(), "GABINETE_" + sanitize(topology.judicialOrganCode()) + '_' + sanitize(topology.laneAxis()));
        String gabineteInboxKey = deriveGabineteInboxKey(firstNonBlank(topology.baseInboxKey(), secretariatRouting.receiptInboxKey()), topology);
        String advisoryDesk = firstNonBlank(topology.assistantDesk(), "ASSESSORIA_" + sanitize(topology.judicialOrganCode()) + '_' + sanitize(topology.laneAxis()));
        String hearingDesk = firstNonBlank(topology.hearingDesk(), "AUDIENCIA_" + sanitize(topology.judicialOrganCode()) + '_' + sanitize(topology.laneAxis()));
        String coordinationDesk = firstNonBlank(topology.coordinationDesk(), "COORD_GABINETE_" + sanitize(topology.judicialOrganCode()) + '_' + sanitize(topology.laneAxis()));
        String redistributionDesk = firstNonBlank(topology.redistributionDesk(), "REDIST_GABINETE_" + sanitize(topology.judicialOrganCode()) + '_' + sanitize(topology.laneAxis()));
        String routeKey = sanitize(topology.topologyKey()) + ':' + sanitize(secretariatRouting.routeKey());
        Duration captureSla = resolveCaptureSla(processo, secretariatRouting, topology);
        List<String> labels = new ArrayList<>();
        labels.add(firstNonBlank(topology.instanceAxis(), "PRIMEIRO_GRAU"));
        labels.add(firstNonBlank(topology.laneAxis(), secretariatRouting.ramoAxis(), "COMUM"));
        labels.add(firstNonBlank(secretariatRouting.tipoJustica(), "ESTADUAL"));
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            labels.add("SIGILO_REFORCADO");
        }
        if (topology.coverage() != null && !Objects.equals(normalize(topology.coverage().sourceMunicipality()), normalize(topology.coverage().seatMunicipality()))) {
            labels.add("COBERTURA_SEDE_DISTINTA");
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("judicialOrganCode", topology.judicialOrganCode());
        metadata.put("judicialOrganKind", topology.judicialOrganKind());
        metadata.put("forumAxis", topology.forumAxis());
        metadata.put("gabineteDescriptor", gabineteDesk + ':' + advisoryDesk + ':' + coordinationDesk);
        metadata.put("territorialScope", topology.coverage() == null ? null : topology.coverage().territorialScope());
        metadata.put("seatMunicipality", topology.coverage() == null ? null : topology.coverage().seatMunicipality());
        metadata.put("coverageMode", topology.coverage() == null ? null : topology.coverage().coverageMode());
        metadata.put("redistributionDesk", redistributionDesk);
        metadata.put("secretariatCode", secretariatRouting.secretariatCode());
        metadata.put("secretariatExecutionQueueCode", secretariatRouting.executionQueueCode());
        metadata.put("secretariatAudienceQueueCode", secretariatRouting.audienceQueueCode());
        metadata.put("secretariatSaneamentoQueueCode", secretariatRouting.saneamentoQueueCode());
        metadata.put("baseInboxKey", topology.baseInboxKey());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        String sessionChannel = topology.instanceAxis() != null && topology.instanceAxis().contains("SEGUNDO") ? "COLEGIADO_GABINETE" : "GABINETE_SINGULAR";
        return new JuizGabineteRoutingProfile(
                routeKey,
                gabineteDesk,
                gabineteInboxKey,
                advisoryDesk,
                hearingDesk,
                coordinationDesk,
                redistributionDesk,
                sessionChannel,
                topology.organizationalPath(),
                captureSla,
                List.copyOf(labels),
                Collections.unmodifiableMap(metadata),
                topology,
                secretariatRouting
        );
    }

    private Duration resolveCaptureSla(Processo processo,
                                       SecretariatOperationalRoutingProfile secretariatRouting,
                                       NationalJudicialTopologyProfile topology) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return Duration.ofHours(2);
        }
        if (topology.instanceAxis() != null && topology.instanceAxis().contains("SEGUNDO")) {
            return Duration.ofHours(6);
        }
        if (secretariatRouting.ramoAxis() != null && (secretariatRouting.ramoAxis().contains("PENAL") || secretariatRouting.ramoAxis().contains("INFANCIA"))) {
            return Duration.ofHours(4);
        }
        return Duration.ofHours(8);
    }

    private String deriveGabineteInboxKey(String baseInboxKey, NationalJudicialTopologyProfile topology) {
        if (baseInboxKey != null && baseInboxKey.startsWith("SEC:")) {
            String replaced = "GAB:" + baseInboxKey.substring(4);
            if (replaced.length() <= 120) {
                return replaced;
            }
        }
        String compact = "GAB:" + sanitize(firstNonBlank(topology.judicialOrganCode(), "ORG")) + ':'
                + sanitize(firstNonBlank(topology.instanceAxis(), "1G")) + ':'
                + sanitize(firstNonBlank(topology.laneAxis(), "COM")) + ':'
                + sanitize(firstNonBlank(topology.coverage() == null ? null : topology.coverage().sourceUf(), "BR")) + ':'
                + Integer.toHexString(Objects.hash(topology.topologyKey(), topology.secretariatUnitCode()));
        return compact.length() > 120 ? compact.substring(0, 120) : compact;
    }

    private String firstNonBlank(String... values) {
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

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "BASE";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String normalize(String raw) {
        return sanitize(raw);
    }
}
