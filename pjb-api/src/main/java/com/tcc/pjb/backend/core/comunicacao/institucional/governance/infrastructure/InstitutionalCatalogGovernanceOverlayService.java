package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceSummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCompetenceRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalRequest;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;

@Service
public class InstitutionalCatalogGovernanceOverlayService {

    private final InstitutionalCatalogGovernanceStateRepository governanceRepository;
    private final InstitutionalCompetenceRuleStateRepository competenceRuleRepository;
    private final Clock clock;

    public InstitutionalCatalogGovernanceOverlayService(InstitutionalCatalogGovernanceStateRepository governanceRepository,
                                                        InstitutionalCompetenceRuleStateRepository competenceRuleRepository,
                                                        Clock clock) {
        this.governanceRepository = Objects.requireNonNull(governanceRepository, "governanceRepository");
        this.competenceRuleRepository = Objects.requireNonNull(competenceRuleRepository, "competenceRuleRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Optional<InstitutionalCatalogGovernanceEntry> effectiveEntry(UnidadeInstitucional unit) {
        Instant now = clock.instant();
        return governanceRepository.findByUnitCode(unit.codigo()).stream()
                .filter(entry -> entry.matches(unit.codigo(), unit.destinatarioKind(), unit.uf(), unit.comarca(), unit.foro(), unit.ramoDireito(), unit.grauJurisdicao(), now))
                .sorted(Comparator.comparing(InstitutionalCatalogGovernanceEntry::vigenciaInicio).reversed())
                .findFirst();
    }

    public UnidadeInstitucional apply(UnidadeInstitucional unit) {
        Optional<InstitutionalCatalogGovernanceEntry> governance = effectiveEntry(unit);
        if (governance.isEmpty()) {
            return unit;
        }
        InstitutionalCatalogGovernanceEntry entry = governance.get();
        List<CanalEntregaInstitucional> baseChannels = unit.canais() == null ? List.of() : unit.canais();
        List<CanalEntregaInstitucional> channels = entry.canaisPreferenciais().isEmpty()
                ? sortChannels(baseChannels)
                : sortChannels(baseChannels.stream().filter(channel -> entry.canaisPreferenciais().contains(channel.canal())).toList());
        return new UnidadeInstitucional(
                unit.codigo(),
                unit.destinatarioKind(),
                unit.sigla(),
                unit.nomeOficial(),
                unit.uf(),
                unit.comarca(),
                unit.foro(),
                unit.unidade(),
                unit.nucleo(),
                unit.ramoDireito(),
                unit.grauJurisdicao(),
                unit.papelPrincipal(),
                unit.caixaPrincipal(),
                channels.isEmpty() ? sortChannels(baseChannels) : channels,
                unit.tribunalCodigo(),
                entry.ativa(),
                mergeNote(unit.observacao(), entry)
        );
    }


    private static List<CanalEntregaInstitucional> sortChannels(List<CanalEntregaInstitucional> channels) {
        if (channels == null || channels.isEmpty()) {
            return List.of();
        }
        java.util.LinkedHashMap<String, CanalEntregaInstitucional> sanitized = new java.util.LinkedHashMap<>();
        for (CanalEntregaInstitucional channel : channels) {
            if (channel != null && channel.canal() != null) {
                sanitized.putIfAbsent(channel.canal().name(), channel);
            }
        }
        ArrayList<CanalEntregaInstitucional> ordered = new ArrayList<>(sanitized.values());
        ordered.sort(Comparator.comparing(channel -> channel.canal().name()));
        return List.copyOf(ordered);
    }

    public Optional<String> preferredUnitCode(ResolucaoRoteamentoInstitucionalRequest request) {
        Instant now = clock.instant();
        return competenceRuleRepository.findEffectiveAt(now).stream()
                .filter(rule -> rule.matches(request.destinatarioKind(), request.papelProcessual(), request.uf(), request.comarca(), request.foro(), request.ramoDireito(), request.grauJurisdicao(), now))
                .sorted(Comparator.comparingInt(InstitutionalCompetenceRule::prioridade)
                        .reversed()
                        .thenComparing(InstitutionalCompetenceRule::updatedAt, Comparator.reverseOrder()))
                .map(InstitutionalCompetenceRule::unidadeCodigo)
                .findFirst();
    }

    public List<InstitutionalCatalogGovernanceEntry> listGovernances(DestinatarioInstitucionalKind destinatarioKind, String uf) {
        return governanceRepository.findAll().stream()
                .filter(entry -> destinatarioKind == null || entry.destinatarioKind() == destinatarioKind)
                .filter(entry -> uf == null || uf.isBlank() || uf.equalsIgnoreCase(entry.uf()))
                .sorted(Comparator.comparing(InstitutionalCatalogGovernanceEntry::updatedAt).reversed())
                .toList();
    }

    public List<InstitutionalCompetenceRule> listCompetenceRules(DestinatarioInstitucionalKind destinatarioKind, String uf) {
        return competenceRuleRepository.findAll().stream()
                .filter(rule -> destinatarioKind == null || rule.destinatarioKind() == destinatarioKind)
                .filter(rule -> uf == null || uf.isBlank() || uf.equalsIgnoreCase(rule.uf()))
                .toList();
    }

    public InstitutionalCatalogGovernanceSummary summarize(long totalCatalogUnits, String catalogVersion) {
        Instant now = clock.instant();
        Instant expiringWindow = now.plus(Duration.ofDays(30));
        List<InstitutionalCatalogGovernanceEntry> governances = governanceRepository.findAll();
        List<InstitutionalCompetenceRule> rules = competenceRuleRepository.findAll();
        long activeGovernances = governances.stream().filter(entry -> entry.isEffectiveAt(now)).count();
        long activeRules = rules.stream().filter(rule -> rule.isEffectiveAt(now)).count();
        long suspended = governances.stream().filter(entry -> !entry.ativa() && !now.isBefore(entry.vigenciaInicio()) && (entry.vigenciaFim() == null || now.isBefore(entry.vigenciaFim()))).count();
        long substitution = governances.stream().filter(entry -> entry.unidadeSubstitutaCodigo() != null && !entry.unidadeSubstitutaCodigo().isBlank()).count();
        long expiring = governances.stream().filter(entry -> entry.vigenciaFim() != null && !entry.vigenciaFim().isBefore(now) && entry.vigenciaFim().isBefore(expiringWindow)).count();
        return new InstitutionalCatalogGovernanceSummary(totalCatalogUnits, activeGovernances, activeRules, suspended, substitution, expiring, catalogVersion);
    }

    public InstitutionalCatalogGovernanceEntry saveGovernance(InstitutionalCatalogGovernanceEntry entry) {
        return governanceRepository.save(entry);
    }

    public InstitutionalCompetenceRule saveCompetenceRule(InstitutionalCompetenceRule rule) {
        return competenceRuleRepository.save(rule);
    }

    private String mergeNote(String current, InstitutionalCatalogGovernanceEntry entry) {
        String suffix = "governança=" + entry.governanceId() + ", abrangencia=" + entry.abrangencia().name();
        if (current == null || current.isBlank()) {
            return suffix;
        }
        return current + " | " + suffix;
    }
}
