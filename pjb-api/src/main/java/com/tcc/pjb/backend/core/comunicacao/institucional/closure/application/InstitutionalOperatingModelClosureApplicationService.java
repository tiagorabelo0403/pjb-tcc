package com.tcc.pjb.backend.core.comunicacao.institucional.closure.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingAdministrativeSeat;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingCoverageRoute;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingModelClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalOperatingRoleBand;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class InstitutionalOperatingModelClosureApplicationService {

    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;
    private final Function<DestinatarioInstitucionalKind, List<UnidadeInstitucional>> unitResolver;

    @Inject
    @Autowired
    public InstitutionalOperatingModelClosureApplicationService(InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService,
                                                                CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService) {
        this(blueprintCatalogApplicationService, catalogoInstitucionalUnificadoService::listarPorTipo);
    }

    InstitutionalOperatingModelClosureApplicationService(InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService,
                                                         Function<DestinatarioInstitucionalKind, List<UnidadeInstitucional>> unitResolver) {
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
        this.unitResolver = Objects.requireNonNull(unitResolver);
    }

    public InstitutionalOperatingModelClosure consolidar(InstitutionalAffiliation affiliation,
                                                         List<InstitutionalNomination> nominations,
                                                         DestinatarioInstitucionalKind requestedKind,
                                                         String municipio,
                                                         String uf) {
        DestinatarioInstitucionalKind effectiveKind = resolveKind(affiliation, requestedKind, nominations);
        InstitutionalOrganizationScope scope = resolveScope(affiliation, effectiveKind);
        InstitutionalOrganizationBlueprint blueprint = blueprintCatalogApplicationService.resolve(scope, effectiveKind).orElse(null);
        InstitutionalOperatingCoverageRoute coverageRoute = resolveCoverage(effectiveKind, affiliation, municipio, uf);
        List<InstitutionalOperatingAdministrativeSeat> seats = buildAdministrativeSeats(blueprint);
        List<InstitutionalOperatingRoleBand> roleBands = buildRoleBands(blueprint, nominations);
        LinkedHashSet<String> findings = buildFindings(affiliation, blueprint, coverageRoute, nominations);
        LinkedHashSet<String> fundamentos = buildFundamentos(affiliation, blueprint, coverageRoute, seats, roleBands);
        return new InstitutionalOperatingModelClosure(
                affiliation == null ? null : affiliation.affiliationId(),
                affiliation == null ? null : affiliation.orgaoSigla(),
                affiliation == null ? null : affiliation.orgaoNome(),
                effectiveKind == null ? null : effectiveKind.name(),
                scope == null ? null : scope.name(),
                affiliation != null && affiliation.blueprintCode() != null ? affiliation.blueprintCode() : blueprint == null ? null : blueprint.codigo(),
                blueprint == null ? null : blueprint.entryMode().name(),
                blueprint != null,
                true,
                magistratesEnterThroughForumAndPersonalAccess(scope, roleBands),
                coverageRoute.coverageMode(),
                coverageRoute,
                seats,
                roleBands,
                List.copyOf(findings),
                List.copyOf(fundamentos),
                Instant.now());
    }

    private DestinatarioInstitucionalKind resolveKind(InstitutionalAffiliation affiliation,
                                                      DestinatarioInstitucionalKind requestedKind,
                                                      List<InstitutionalNomination> nominations) {
        if (affiliation != null && affiliation.destinatarioKind() != null) {
            return affiliation.destinatarioKind();
        }
        if (requestedKind != null) {
            return requestedKind;
        }
        if (nominations != null) {
            Optional<TipoUsuario> tipo = nominations.stream().map(InstitutionalNomination::tipoUsuario).filter(Objects::nonNull).findFirst();
            if (tipo.isPresent()) {
                return inferKindFromTipoUsuario(tipo.get());
            }
        }
        return DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO;
    }

    private InstitutionalOrganizationScope resolveScope(InstitutionalAffiliation affiliation,
                                                        DestinatarioInstitucionalKind kind) {
        if (affiliation != null && affiliation.organizationScope() != null) {
            return affiliation.organizationScope();
        }
        return blueprintCatalogApplicationService.inferScope(
                kind,
                affiliation == null ? null : affiliation.unidadeCodigo(),
                affiliation == null ? null : affiliation.orgaoSigla(),
                affiliation == null ? null : affiliation.unidadeNome());
    }

    private InstitutionalOperatingCoverageRoute resolveCoverage(DestinatarioInstitucionalKind kind,
                                                                InstitutionalAffiliation affiliation,
                                                                String municipio,
                                                                String uf) {
        List<UnidadeInstitucional> unidades = kind == null ? List.of() : unitResolver.apply(kind).stream().filter(UnidadeInstitucional::ativa).toList();
        String requestedMunicipality = firstNonBlank(municipio, affiliation == null ? null : affiliation.comarca(), affiliation == null ? null : affiliation.unidadeNome());
        String requestedUf = firstNonBlank(uf, affiliation == null ? null : affiliation.uf());
        UnidadeInstitucional byAffiliationCode = affiliation == null || affiliation.unidadeCodigo() == null ? null : unidades.stream()
                .filter(item -> item.codigo().equalsIgnoreCase(affiliation.unidadeCodigo()))
                .findFirst()
                .orElse(null);
        UnidadeInstitucional local = unidades.stream()
                .filter(item -> matchesUf(item, requestedUf))
                .filter(item -> matchesTerritory(item, requestedMunicipality))
                .sorted(Comparator.comparing(UnidadeInstitucional::codigo))
                .findFirst()
                .orElse(byAffiliationCode);
        List<String> fallbackChain = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalOperatingModelMessages.requestedTerritory(requestedMunicipality, requestedUf));
        UnidadeInstitucional responsible = local;
        String mode;
        boolean localPresent;
        if (local != null && requestedMunicipality != null && matchesTerritory(local, requestedMunicipality)) {
            mode = "LOCAL";
            localPresent = true;
            fallbackChain.add("MUNICIPIO_LOCAL");
            fundamentos.add(InstitutionalOperatingModelMessages.LOCAL_COVERAGE);
        } else {
            UnidadeInstitucional sameUf = unidades.stream()
                    .filter(item -> matchesUf(item, requestedUf))
                    .sorted(Comparator.comparingInt((UnidadeInstitucional item) -> sameCourtScore(item, byAffiliationCode)).reversed()
                            .thenComparing(item -> territoryScore(item, requestedMunicipality)).reversed()
                            .thenComparing(UnidadeInstitucional::codigo))
                    .findFirst()
                    .orElse(null);
            responsible = sameUf != null ? sameUf : local;
            localPresent = false;
            if (sameUf != null) {
                mode = "SEDE_COMPETENTE_UF";
                fallbackChain.add("SEDE_COMPETENTE_UF");
                if (requestedMunicipality != null) {
                    fallbackChain.add("MUNICIPIO_ATENDIDO_POR_SEDE");
                }
                fundamentos.add(InstitutionalOperatingModelMessages.MUNICIPAL_FALLBACK);
            } else if (responsible != null) {
                mode = "COBERTURA_RESIDUAL";
                fallbackChain.add("COBERTURA_RESIDUAL_CATALOGO");
                fundamentos.add(InstitutionalOperatingModelMessages.NATIONAL_FALLBACK_FINDING);
            } else {
                mode = "SEM_CORRESPONDENCIA_CATALOGO";
                fallbackChain.add("SEM_CORRESPONDENCIA_CATALOGO");
                fundamentos.add(InstitutionalOperatingModelMessages.NATIONAL_FALLBACK_FINDING);
            }
        }
        if (responsible != null) {
            fundamentos.add(InstitutionalOperatingModelMessages.responsibleUnit(responsible.codigo()));
        }
        fundamentos.add(InstitutionalOperatingModelMessages.coverageMode(mode));
        return new InstitutionalOperatingCoverageRoute(
                requestedMunicipality,
                requestedUf,
                kind == null ? null : kind.name(),
                localPresent,
                responsible == null ? null : responsible.codigo(),
                responsible == null ? null : responsible.nomeOficial(),
                responsible == null ? null : responsible.foro(),
                responsible == null ? null : responsible.comarca(),
                responsible == null ? null : responsible.tribunalCodigo(),
                mode,
                List.copyOf(fallbackChain),
                List.copyOf(fundamentos));
    }

    private List<InstitutionalOperatingAdministrativeSeat> buildAdministrativeSeats(InstitutionalOrganizationBlueprint blueprint) {
        if (blueprint == null) {
            return List.of();
        }
        return blueprint.lanes().stream()
                .map(this::toSeat)
                .sorted(Comparator.comparing(InstitutionalOperatingAdministrativeSeat::code, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private InstitutionalOperatingAdministrativeSeat toSeat(InstitutionalAccessLaneBlueprint lane) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalOperatingModelMessages.seat(lane.codigo()));
        fundamentos.addAll(lane.fundamentos());
        return new InstitutionalOperatingAdministrativeSeat(
                lane.codigo(),
                lane.nomeExibicao(),
                lane.laneKind() == null ? null : lane.laneKind().name(),
                lane.nominationRole() == null ? null : lane.nominationRole().name(),
                lane.processProfile() == null ? null : lane.processProfile().name(),
                lane.trustFloor() == null ? null : lane.trustFloor().name(),
                lane.nominationRole() != null && lane.nominationRole().isGestaoMestre(),
                lane.requerStepUpMfa(),
                lane.requerCertificadoICP(),
                lane.permiteUsoRemotoAutorizado(),
                lane.capacidadesPadrao().stream().map(Enum::name).sorted().toList(),
                lane.restricoes(),
                List.copyOf(fundamentos));
    }

    private List<InstitutionalOperatingRoleBand> buildRoleBands(InstitutionalOrganizationBlueprint blueprint,
                                                                List<InstitutionalNomination> nominations) {
        LinkedHashMap<String, RoleBandAccumulator> bands = new LinkedHashMap<>();
        if (blueprint != null) {
            for (InstitutionalAccessLaneBlueprint lane : blueprint.lanes()) {
                String key = lane.laneKind() == null ? lane.codigo() : lane.laneKind().name();
                bands.putIfAbsent(key, new RoleBandAccumulator(
                        key,
                        lane.laneKind() == null ? null : lane.laneKind().name(),
                        lane.nominationRole() == null ? null : lane.nominationRole().name(),
                        null,
                        lane.nomeExibicao(),
                        0,
                        false,
                        true,
                        false,
                        new LinkedHashSet<>(lane.capacidadesPadrao().stream().map(Enum::name).toList()),
                        new LinkedHashSet<>(lane.fundamentos())));
            }
        }
        if (nominations != null) {
            for (InstitutionalNomination nomination : nominations) {
                String key = nomination.accessLaneKind() == null ? nomination.nominationRole().name() : nomination.accessLaneKind().name();
                RoleBandAccumulator current = bands.getOrDefault(key, new RoleBandAccumulator(
                        key,
                        nomination.accessLaneKind() == null ? null : nomination.accessLaneKind().name(),
                        nomination.nominationRole() == null ? null : nomination.nominationRole().name(),
                        nomination.tipoUsuario() == null ? null : nomination.tipoUsuario().name(),
                        nomination.panelPreferencial() == null ? key : nomination.panelPreferencial().name(),
                        0,
                        false,
                        true,
                        false,
                        new LinkedHashSet<>(),
                        new LinkedHashSet<>()));
                current.activeNominations++;
                if (nomination.tipoUsuario() != null) {
                    current.tipoUsuario = nomination.tipoUsuario().name();
                    current.judicialAuthority = current.judicialAuthority || nomination.tipoUsuario().isMagistratura();
                    current.personalDirectEntryAllowed = current.personalDirectEntryAllowed || nomination.tipoUsuario().isMagistratura() || nomination.tipoUsuario().isAdvocacia() || nomination.tipoUsuario() == TipoUsuario.CIDADAO;
                    current.institutionalOnly = !(nomination.tipoUsuario().isMagistratura() || nomination.tipoUsuario().isAdvocacia() || nomination.tipoUsuario() == TipoUsuario.CIDADAO);
                }
                current.capacities.addAll(nomination.capacidades().stream().map(Enum::name).toList());
                current.fundamentos.add(InstitutionalOperatingModelMessages.roleBand(key));
                current.fundamentos.add(InstitutionalOperatingModelMessages.nominationCount(current.activeNominations));
                bands.put(key, current);
            }
        }
        return bands.values().stream()
                .map(RoleBandAccumulator::toRecord)
                .sorted(Comparator.comparing(InstitutionalOperatingRoleBand::bandKey))
                .toList();
    }

    private LinkedHashSet<String> buildFindings(InstitutionalAffiliation affiliation,
                                                InstitutionalOrganizationBlueprint blueprint,
                                                InstitutionalOperatingCoverageRoute coverageRoute,
                                                List<InstitutionalNomination> nominations) {
        LinkedHashSet<String> findings = new LinkedHashSet<>();
        if (affiliation == null || !affiliation.ativa()) {
            findings.add(InstitutionalOperatingModelMessages.AFFILIATION_INACTIVE);
        }
        boolean hasMasterAdmin = nominations != null && nominations.stream()
                .filter(item -> item.nominationRole() != null)
                .anyMatch(item -> item.nominationRole().isGestaoMestre() && item.ativaEm(Instant.now()));
        if (blueprint != null && blueprint.entryMode() != null && !hasMasterAdmin) {
            findings.add(InstitutionalOperatingModelMessages.MASTER_ADMIN_MISSING);
        }
        boolean hasTitular = nominations != null && nominations.stream()
                .filter(item -> item.tipoUsuario() != null)
                .anyMatch(item -> item.ativaEm(Instant.now()) && (item.tipoUsuario().isMagistratura() || item.tipoUsuario().isMinisterioPublico() || item.tipoUsuario().isDefensoriaPublica() || item.tipoUsuario().isProcuradoria()));
        if (blueprint != null && blueprint.destinatarioKind() != null && blueprint.destinatarioKind().isInstituicaoEssencialJustica() && !hasTitular) {
            findings.add(InstitutionalOperatingModelMessages.TITULAR_MISSING);
        }
        if (!coverageRoute.localUnitPresent() && "SEDE_COMPETENTE_UF".equalsIgnoreCase(coverageRoute.coverageMode())) {
            findings.add(InstitutionalOperatingModelMessages.MUNICIPALITY_FALLBACK_FINDING);
        }
        if ("SEM_CORRESPONDENCIA_CATALOGO".equalsIgnoreCase(coverageRoute.coverageMode())) {
            findings.add(InstitutionalOperatingModelMessages.NATIONAL_FALLBACK_FINDING);
        }
        return findings;
    }

    private LinkedHashSet<String> buildFundamentos(InstitutionalAffiliation affiliation,
                                                   InstitutionalOrganizationBlueprint blueprint,
                                                   InstitutionalOperatingCoverageRoute coverageRoute,
                                                   List<InstitutionalOperatingAdministrativeSeat> seats,
                                                   List<InstitutionalOperatingRoleBand> roleBands) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add(InstitutionalOperatingModelMessages.PERSONAL_ROOT_IDENTITY);
        fundamentos.add(InstitutionalOperatingModelMessages.INSTITUTION_OWNS_SEATS);
        fundamentos.add(InstitutionalOperatingModelMessages.PJB_HOMOLOGATES);
        fundamentos.add(InstitutionalOperatingModelMessages.SHARED_ACCOUNT_FORBIDDEN);
        if (affiliation != null) {
            fundamentos.addAll(affiliation.fundamentos());
            fundamentos.add(InstitutionalOperatingModelMessages.governanceAnchor(affiliation.organizationScope() == null ? null : affiliation.organizationScope().name()));
            fundamentos.add(InstitutionalOperatingModelMessages.blueprint(firstNonBlank(affiliation.blueprintCode(), blueprint == null ? null : blueprint.codigo())));
        }
        if (blueprint != null) {
            fundamentos.addAll(blueprint.fundamentos());
            fundamentos.add(InstitutionalOperatingModelMessages.entryMode(blueprint.entryMode().name()));
        }
        if (magistratesEnterThroughForumAndPersonalAccess(affiliation == null ? null : affiliation.organizationScope(), roleBands)) {
            fundamentos.add(InstitutionalOperatingModelMessages.JUDGE_PERSONAL_ENTRY);
        }
        fundamentos.addAll(coverageRoute.fundamentos());
        seats.stream().flatMap(item -> item.fundamentos().stream()).forEach(fundamentos::add);
        roleBands.stream().flatMap(item -> item.fundamentos().stream()).forEach(fundamentos::add);
        return fundamentos;
    }

    private boolean magistratesEnterThroughForumAndPersonalAccess(InstitutionalOrganizationScope scope,
                                                                  List<InstitutionalOperatingRoleBand> roleBands) {
        if (scope == InstitutionalOrganizationScope.FORUM || scope == InstitutionalOrganizationScope.SECRETARIA_UNIDADE_JUDICIARIA) {
            return true;
        }
        return roleBands.stream().anyMatch(item -> item.judicialAuthority() && item.personalDirectEntryAllowed());
    }

    private DestinatarioInstitucionalKind inferKindFromTipoUsuario(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO;
        }
        if (tipoUsuario.isMinisterioPublico()) {
            return DestinatarioInstitucionalKind.MINISTERIO_PUBLICO;
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            return DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA;
        }
        if (tipoUsuario.isProcuradoria()) {
            return DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA;
        }
        if (tipoUsuario.isSegurancaPublica()) {
            return DestinatarioInstitucionalKind.DELEGACIA_POLICIA;
        }
        if (tipoUsuario.isPerito()) {
            return DestinatarioInstitucionalKind.PERITO_JUDICIAL;
        }
        return DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO;
    }

    private boolean matchesTerritory(UnidadeInstitucional unidade, String municipality) {
        if (municipality == null || municipality.isBlank()) {
            return false;
        }
        String normalized = normalize(municipality);
        return normalized.equals(normalize(unidade.comarca()))
                || normalized.equals(normalize(unidade.foro()))
                || normalized.equals(normalize(unidade.unidade()))
                || normalized.equals(normalize(unidade.nucleo()))
                || normalized.equals(normalize(unidade.nomeOficial()));
    }

    private boolean matchesUf(UnidadeInstitucional unidade, String uf) {
        if (uf == null || uf.isBlank()) {
            return true;
        }
        return unidade.uf() != null && unidade.uf().equalsIgnoreCase(uf.trim());
    }

    private int territoryScore(UnidadeInstitucional unidade, String municipality) {
        if (municipality == null || municipality.isBlank()) {
            return 0;
        }
        String normalized = normalize(municipality);
        if (normalized.equals(normalize(unidade.comarca())) || normalized.equals(normalize(unidade.foro()))) {
            return 5;
        }
        if (normalized.equals(normalize(unidade.unidade())) || normalized.equals(normalize(unidade.nucleo()))) {
            return 3;
        }
        return 0;
    }

    private int sameCourtScore(UnidadeInstitucional candidate, UnidadeInstitucional preferred) {
        if (candidate == null || preferred == null) {
            return 0;
        }
        return Objects.equals(candidate.tribunalCodigo(), preferred.tribunalCodigo()) ? 4 : 0;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
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

    private static final class RoleBandAccumulator {
        private final String bandKey;
        private String laneKind;
        private String nominationRole;
        private String tipoUsuario;
        private String displayName;
        private long activeNominations;
        private boolean judicialAuthority;
        private boolean institutionalOnly;
        private boolean personalDirectEntryAllowed;
        private final LinkedHashSet<String> capacities;
        private final LinkedHashSet<String> fundamentos;

        private RoleBandAccumulator(String bandKey,
                                    String laneKind,
                                    String nominationRole,
                                    String tipoUsuario,
                                    String displayName,
                                    long activeNominations,
                                    boolean judicialAuthority,
                                    boolean institutionalOnly,
                                    boolean personalDirectEntryAllowed,
                                    LinkedHashSet<String> capacities,
                                    LinkedHashSet<String> fundamentos) {
            this.bandKey = bandKey;
            this.laneKind = laneKind;
            this.nominationRole = nominationRole;
            this.tipoUsuario = tipoUsuario;
            this.displayName = displayName;
            this.activeNominations = activeNominations;
            this.judicialAuthority = judicialAuthority;
            this.institutionalOnly = institutionalOnly;
            this.personalDirectEntryAllowed = personalDirectEntryAllowed;
            this.capacities = capacities;
            this.fundamentos = fundamentos;
        }

        private InstitutionalOperatingRoleBand toRecord() {
            return new InstitutionalOperatingRoleBand(
                    bandKey,
                    laneKind,
                    nominationRole,
                    tipoUsuario,
                    displayName,
                    activeNominations,
                    judicialAuthority,
                    institutionalOnly,
                    personalDirectEntryAllowed,
                    capacities.stream().filter(Objects::nonNull).sorted().toList(),
                    fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().collect(Collectors.toList()));
        }
    }
}
