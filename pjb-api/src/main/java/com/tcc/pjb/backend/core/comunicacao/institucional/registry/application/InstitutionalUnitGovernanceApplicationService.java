package com.tcc.pjb.backend.core.comunicacao.institucional.registry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalLotationGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalManagedUnitEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalProvisioningSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalProvisionedDirectoryEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.infrastructure.InstitutionalUnitGovernanceStateRepository;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalLotationUpsertRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalManagedUnitUpsertRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalUnitGovernanceApplicationService {

    private final InstitutionalUnitGovernanceStateRepository repository;
    private final InstitutionalOperationalProvisioningApplicationService provisioningApplicationService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;

    public InstitutionalUnitGovernanceApplicationService(InstitutionalUnitGovernanceStateRepository repository,
                                                         InstitutionalOperationalProvisioningApplicationService provisioningApplicationService,
                                                         InstitutionalAffiliationStateRepository affiliationRepository,
                                                         InstitutionalNominationStateRepository nominationRepository) {
        this.repository = Objects.requireNonNull(repository);
        this.provisioningApplicationService = Objects.requireNonNull(provisioningApplicationService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
    }

    public InstitutionalUnitGovernanceSnapshot consolidar(String affiliationId) {
        return repository.findLatestByAffiliationId(affiliationId).orElseGet(() -> buildBaseSnapshot(affiliationId, List.of()));
    }

    public InstitutionalUnitGovernanceSnapshot registrarUnidade(String affiliationId,
                                                                NationalCommunicationInstitutionalManagedUnitUpsertRequest request) {
        Objects.requireNonNull(request, "Requisição de unidade é obrigatória.");
        InstitutionalUnitGovernanceSnapshot current = consolidar(affiliationId);
        ArrayList<InstitutionalManagedUnitEntry> units = new ArrayList<>(current.units());
        String unitCode = normalizeCode(request.unitCode(), current.affiliationId() + ":UNIT");
        InstitutionalManagedUnitEntry incoming = new InstitutionalManagedUnitEntry(
                unitCode,
                safe(request.unitName(), "Unidade institucional"),
                blankToNull(request.parentUnitCode()),
                blankToNull(request.territorialScope()),
                blankToNull(request.municipalityCoverage()),
                safe(request.defaultBoxCode(), unitCode + ":PRINCIPAL"),
                current.affiliationId() + '|' + unitCode,
                replicaCode(current.affiliationId(), unitCode),
                true,
                !Boolean.FALSE.equals(request.homologated()),
                sanitizeList(request.boxes(), List.of(safe(request.defaultBoxCode(), unitCode + ":PRINCIPAL"))),
                sanitizeList(request.laneCodes(), List.of()),
                List.of());
        int index = indexOfUnit(units, unitCode);
        if (index >= 0) {
            units.set(index, incoming);
        } else {
            units.add(incoming);
        }
        ArrayList<String> fundamentos = new ArrayList<>(current.fundamentos());
        fundamentos.add("governanca_unidade_atualizada=" + unitCode);
        fundamentos.addAll(sanitizeList(request.fundamentos(), List.of()));
        return repository.save(rebuild(current, units, new ArrayList<>(current.lotacoes()), fundamentos));
    }

    public InstitutionalUnitGovernanceSnapshot registrarLotacao(String affiliationId,
                                                                NationalCommunicationInstitutionalLotationUpsertRequest request) {
        Objects.requireNonNull(request, "Requisição de lotação é obrigatória.");
        InstitutionalUnitGovernanceSnapshot current = consolidar(affiliationId);
        InstitutionalNomination nomination = request.nominationId() == null || request.nominationId().isBlank()
                ? null
                : nominationRepository.findByNominationId(request.nominationId()).orElse(null);
        String unitCode = firstNonBlank(request.unitCode(), nomination == null ? null : nomination.unidadeCodigo(), current.units().isEmpty() ? null : current.units().getFirst().unitCode());
        String boxCode = firstNonBlank(request.boxCode(), nomination == null ? null : nomination.caixaCodigo(), defaultBox(current.units(), unitCode));
        ArrayList<String> findings = new ArrayList<>();
        if (unitCode == null || unitCode.isBlank()) {
            findings.add("unidade_nao_resolvida_para_lotacao");
        }
        if (current.units().stream().noneMatch(item -> Objects.equals(item.unitCode(), unitCode))) {
            findings.add("unidade_ainda_nao_materializada");
        }
        InstitutionalLotationGovernanceEntry incoming = new InstitutionalLotationGovernanceEntry(
                request.nominationId() != null && !request.nominationId().isBlank() ? request.nominationId() : UUID.randomUUID().toString(),
                request.nominationId(),
                request.userId() != null ? request.userId() : nomination == null ? null : nomination.nominatedUserId(),
                safe(firstNonBlank(request.userName(), nomination == null ? null : nomination.nominatedUserName()), "Usuário institucional"),
                unitCode,
                boxCode,
                firstNonBlank(request.laneCode(), nomination == null || nomination.accessLaneKind() == null ? null : nomination.accessLaneKind().name()),
                firstNonBlank(request.nominationRole(), nomination == null || nomination.nominationRole() == null ? null : nomination.nominationRole().name()),
                firstNonBlank(request.operationalFunction(), nomination == null || nomination.funcaoOperacional() == null ? null : nomination.funcaoOperacional().name()),
                firstNonBlank(request.trustFloor(), nomination == null || nomination.trustFloor() == null ? null : nomination.trustFloor().name()),
                !Boolean.FALSE.equals(request.active()),
                request.activeFrom() != null ? request.activeFrom() : nomination == null ? Instant.now() : nomination.ativaDe(),
                request.activeUntil() != null ? request.activeUntil() : nomination == null ? null : nomination.ativaAte(),
                List.copyOf(findings));
        ArrayList<InstitutionalLotationGovernanceEntry> lotacoes = new ArrayList<>(current.lotacoes());
        int index = indexOfLotacao(lotacoes, incoming.lotationId(), incoming.nominationId());
        if (index >= 0) {
            lotacoes.set(index, incoming);
        } else {
            lotacoes.add(incoming);
        }
        ArrayList<String> fundamentos = new ArrayList<>(current.fundamentos());
        fundamentos.add("governanca_lotacao_atualizada=" + incoming.lotationId());
        fundamentos.addAll(sanitizeList(request.fundamentos(), List.of()));
        return repository.save(rebuild(current, new ArrayList<>(current.units()), lotacoes, fundamentos));
    }

    private InstitutionalUnitGovernanceSnapshot buildBaseSnapshot(String affiliationId, List<String> extraFundamentos) {
        InstitutionalOperationalProvisioningSnapshot provisioning = provisioningApplicationService.consolidar(affiliationId);
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId).orElse(null);
        ArrayList<InstitutionalManagedUnitEntry> units = new ArrayList<>();
        Map<String, List<InstitutionalProvisionedDirectoryEntry>> children = new LinkedHashMap<>();
        for (InstitutionalProvisionedDirectoryEntry entry : provisioning.entries()) {
            if (entry.parentEntryId() != null) {
                children.computeIfAbsent(entry.parentEntryId(), ignored -> new ArrayList<>()).add(entry);
            }
        }
        for (InstitutionalProvisionedDirectoryEntry entry : provisioning.entries()) {
            if (!"UNIDADE".equals(entry.entryType())) {
                continue;
            }
            List<InstitutionalProvisionedDirectoryEntry> childEntries = children.getOrDefault(entry.entryId(), List.of());
            List<String> boxes = childEntries.stream().filter(item -> "CAIXA".equals(item.entryType())).map(InstitutionalProvisionedDirectoryEntry::code).sorted().toList();
            List<String> lanes = childEntries.stream().filter(item -> "LOTACAO".equals(item.entryType())).map(item -> item.findings().isEmpty() ? "SEM_LANE" : item.findings().getFirst()).distinct().sorted().toList();
            units.add(new InstitutionalManagedUnitEntry(
                    entry.code(),
                    entry.displayName(),
                    null,
                    entry.territorialScope(),
                    affiliation == null ? null : affiliation.comarca(),
                    entry.caixaCodigo(),
                    entry.primaryWritePartitionKey(),
                    entry.readReplicaCode(),
                    true,
                    provisioning.affiliationActive(),
                    boxes,
                    lanes,
                    entry.findings()));
        }
        ArrayList<InstitutionalLotationGovernanceEntry> lotacoes = nominationRepository.findByAffiliationId(affiliationId).stream()
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(item -> new InstitutionalLotationGovernanceEntry(
                        item.nominationId(),
                        item.nominationId(),
                        item.nominatedUserId(),
                        item.nominatedUserName(),
                        item.unidadeCodigo(),
                        item.caixaCodigo(),
                        item.accessLaneKind() == null ? null : item.accessLaneKind().name(),
                        item.nominationRole() == null ? null : item.nominationRole().name(),
                        item.funcaoOperacional() == null ? null : item.funcaoOperacional().name(),
                        item.trustFloor() == null ? null : item.trustFloor().name(),
                        item.ativaEm(Instant.now()),
                        item.ativaDe(),
                        item.ativaAte(),
                        List.of()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        ArrayList<String> findings = new ArrayList<>();
        if (!provisioning.affiliationActive()) {
            findings.add("afiliacao_ainda_nao_homologada");
        }
        if (units.isEmpty()) {
            findings.add("nenhuma_unidade_materializada");
        }
        if (lotacoes.isEmpty()) {
            findings.add("nenhuma_lotacao_materializada");
        }
        ArrayList<String> fundamentos = new ArrayList<>(provisioning.fundamentos());
        fundamentos.add("governanca_unidade_lotacao_e_caixa_materializada_a_partir_do_provisionamento");
        fundamentos.addAll(extraFundamentos);
        return new InstitutionalUnitGovernanceSnapshot(
                UUID.randomUUID().toString(),
                provisioning.affiliationId(),
                provisioning.orgaoSigla(),
                provisioning.orgaoNome(),
                provisioning.organizationScope(),
                findings.isEmpty() ? "ATIVA" : provisioning.affiliationActive() ? "ATENCAO" : "PENDENTE_HOMOLOGACAO",
                units.size(),
                units.stream().mapToInt(item -> item.boxes().size()).sum(),
                lotacoes.size(),
                units,
                lotacoes,
                List.copyOf(findings),
                List.copyOf(fundamentos),
                Instant.now());
    }

    private InstitutionalUnitGovernanceSnapshot rebuild(InstitutionalUnitGovernanceSnapshot current,
                                                        ArrayList<InstitutionalManagedUnitEntry> units,
                                                        ArrayList<InstitutionalLotationGovernanceEntry> lotacoes,
                                                        List<String> fundamentos) {
        ArrayList<String> findings = new ArrayList<>();
        if (units.isEmpty()) {
            findings.add("nenhuma_unidade_materializada");
        }
        if (lotacoes.isEmpty()) {
            findings.add("nenhuma_lotacao_materializada");
        }
        return new InstitutionalUnitGovernanceSnapshot(
                UUID.randomUUID().toString(),
                current.affiliationId(),
                current.orgaoSigla(),
                current.orgaoNome(),
                current.organizationScope(),
                findings.isEmpty() ? "ATIVA" : "ATENCAO",
                units.size(),
                units.stream().mapToInt(item -> item.boxes().size()).sum(),
                lotacoes.size(),
                List.copyOf(units),
                List.copyOf(lotacoes),
                List.copyOf(findings),
                List.copyOf(fundamentos),
                Instant.now());
    }

    private int indexOfUnit(List<InstitutionalManagedUnitEntry> units, String unitCode) {
        for (int i = 0; i < units.size(); i++) {
            if (Objects.equals(units.get(i).unitCode(), unitCode)) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfLotacao(List<InstitutionalLotationGovernanceEntry> lotacoes, String lotationId, String nominationId) {
        for (int i = 0; i < lotacoes.size(); i++) {
            InstitutionalLotationGovernanceEntry item = lotacoes.get(i);
            if (Objects.equals(item.lotationId(), lotationId) || (nominationId != null && Objects.equals(item.nominationId(), nominationId))) {
                return i;
            }
        }
        return -1;
    }

    private String defaultBox(List<InstitutionalManagedUnitEntry> units, String unitCode) {
        return units.stream().filter(item -> Objects.equals(item.unitCode(), unitCode)).findFirst().map(InstitutionalManagedUnitEntry::defaultBoxCode).orElse(null);
    }

    private List<String> sanitizeList(List<String> values, List<String> fallback) {
        List<String> out = values == null || values.isEmpty() ? fallback : values;
        return out.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    private String normalizeCode(String raw, String fallback) {
        String source = raw == null || raw.isBlank() ? fallback : raw;
        return source.trim().replaceAll("[^A-Za-z0-9:_-]", "_").toUpperCase(Locale.ROOT);
    }

    private String replicaCode(String affiliationId, String unitCode) {
        String scope = affiliationId == null ? "PJB" : affiliationId.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        String unit = unitCode == null ? "UNIT" : unitCode.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return "RR_" + scope + '_' + unit;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
