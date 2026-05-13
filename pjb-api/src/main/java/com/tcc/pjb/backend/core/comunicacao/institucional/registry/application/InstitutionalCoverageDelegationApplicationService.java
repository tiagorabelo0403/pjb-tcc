package com.tcc.pjb.backend.core.comunicacao.institucional.registry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalLotationGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalManagedUnitEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.infrastructure.InstitutionalCoverageDelegationStateRepository;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalCoverageDelegationUpsertRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalCoverageDelegationApplicationService {

    private final InstitutionalCoverageDelegationStateRepository repository;
    private final InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService;

    public InstitutionalCoverageDelegationApplicationService(InstitutionalCoverageDelegationStateRepository repository,
                                                             InstitutionalUnitGovernanceApplicationService unitGovernanceApplicationService) {
        this.repository = Objects.requireNonNull(repository);
        this.unitGovernanceApplicationService = Objects.requireNonNull(unitGovernanceApplicationService);
    }

    public InstitutionalCoverageDelegationSnapshot consolidar(String affiliationId) {
        return repository.findLatestByAffiliationId(affiliationId)
                .orElseGet(() -> buildBaseSnapshot(affiliationId, List.of(), List.of()));
    }

    public InstitutionalCoverageDelegationSnapshot registrar(String affiliationId,
                                                             NationalCommunicationInstitutionalCoverageDelegationUpsertRequest request) {
        Objects.requireNonNull(request, "Requisição de delegação é obrigatória.");
        InstitutionalCoverageDelegationSnapshot current = consolidar(affiliationId);
        InstitutionalUnitGovernanceSnapshot governance = unitGovernanceApplicationService.consolidar(affiliationId);
        ArrayList<InstitutionalCoverageDelegationEntry> delegations = new ArrayList<>(current.delegations());
        InstitutionalLotationGovernanceEntry source = resolveLotation(governance.lotacoes(), request.sourceLotationId(), request.sourceUserId());
        InstitutionalLotationGovernanceEntry target = resolveLotation(governance.lotacoes(), request.targetLotationId(), request.targetUserId());
        String unitCode = firstNonBlank(request.unitCode(), source == null ? null : source.unitCode(), target == null ? null : target.unitCode());
        String boxCode = firstNonBlank(request.boxCode(), source == null ? null : source.boxCode(), target == null ? null : target.boxCode());
        String laneCode = firstNonBlank(request.laneCode(), source == null ? null : source.laneCode(), target == null ? null : target.laneCode());
        ArrayList<String> findings = new ArrayList<>();
        InstitutionalManagedUnitEntry unit = governance.units().stream().filter(item -> Objects.equals(item.unitCode(), unitCode)).findFirst().orElse(null);
        if (source == null) {
            findings.add("lotacao_origem_nao_localizada");
        }
        if (target == null && request.targetUserId() == null) {
            findings.add("lotacao_destino_nao_localizada");
        }
        if (unit == null) {
            findings.add("unidade_nao_localizada_para_delegacao");
        }
        if (unit != null && boxCode != null && unit.boxes().stream().noneMatch(boxCode::equalsIgnoreCase)) {
            findings.add("caixa_fora_do_catalogo_da_unidade");
        }
        Instant activeFrom = request.activeFrom() != null ? request.activeFrom() : Instant.now();
        Instant activeUntil = request.activeUntil();
        boolean active = !Boolean.FALSE.equals(request.active()) && (activeUntil == null || activeUntil.isAfter(Instant.now()));
        boolean crossMunicipalitySupport = Boolean.TRUE.equals(request.crossMunicipalitySupport())
                || source != null && target != null && !Objects.equals(resolveMunicipality(governance.units(), source.unitCode()), resolveMunicipality(governance.units(), target.unitCode()));
        InstitutionalCoverageDelegationEntry incoming = new InstitutionalCoverageDelegationEntry(
                request.delegationId() == null || request.delegationId().isBlank() ? UUID.randomUUID().toString() : request.delegationId().trim(),
                source == null ? request.sourceLotationId() : source.lotationId(),
                source == null ? request.sourceUserId() : source.userId(),
                safe(firstNonBlank(request.sourceUserName(), source == null ? null : source.userName()), "Origem institucional"),
                target == null ? request.targetLotationId() : target.lotationId(),
                target == null ? request.targetUserId() : target.userId(),
                safe(firstNonBlank(request.targetUserName(), target == null ? null : target.userName()), "Cobertura institucional"),
                unitCode,
                boxCode,
                laneCode,
                safe(normalize(request.delegationKind()), "SUBSTITUICAO_TEMPORARIA"),
                activeFrom,
                activeUntil,
                active,
                crossMunicipalitySupport,
                List.copyOf(findings));
        int index = indexOf(delegations, incoming.delegationId());
        if (index >= 0) {
            delegations.set(index, incoming);
        } else {
            delegations.add(incoming);
        }
        ArrayList<String> fundamentos = new ArrayList<>(current.fundamentos());
        fundamentos.add("delegacao_cobertura_atualizada=" + incoming.delegationId());
        fundamentos.addAll(sanitize(request.fundamentos()));
        return repository.save(buildSnapshot(affiliationId, delegations, fundamentos));
    }

    private InstitutionalCoverageDelegationSnapshot buildBaseSnapshot(String affiliationId,
                                                                      List<String> findings,
                                                                      List<String> fundamentos) {
        return buildSnapshot(affiliationId, List.of(), merge(findings, fundamentos));
    }

    private InstitutionalCoverageDelegationSnapshot buildSnapshot(String affiliationId,
                                                                  List<InstitutionalCoverageDelegationEntry> delegations,
                                                                  List<String> fundamentos) {
        List<InstitutionalCoverageDelegationEntry> safeDelegations = delegations == null ? List.of() : List.copyOf(delegations);
        int activeDelegations = (int) safeDelegations.stream().filter(InstitutionalCoverageDelegationEntry::active).count();
        ArrayList<String> findings = new ArrayList<>();
        if (safeDelegations.isEmpty()) {
            findings.add("delegacao_cobertura_ainda_nao_materializada");
        }
        if (safeDelegations.stream().anyMatch(item -> item.activeUntil() != null && item.activeUntil().isBefore(Instant.now()))) {
            findings.add("delegacao_com_janela_expirada");
        }
        return new InstitutionalCoverageDelegationSnapshot(
                UUID.randomUUID().toString(),
                affiliationId,
                activeDelegations > 0 ? "ATIVA" : safeDelegations.isEmpty() ? "PENDENTE" : "CONFIGURADA",
                safeDelegations.size(),
                activeDelegations,
                safeDelegations,
                List.copyOf(findings),
                sanitize(fundamentos),
                Instant.now());
    }

    private InstitutionalLotationGovernanceEntry resolveLotation(List<InstitutionalLotationGovernanceEntry> lotacoes,
                                                                 String lotationId,
                                                                 Long userId) {
        if (lotacoes == null || lotacoes.isEmpty()) {
            return null;
        }
        if (lotationId != null && !lotationId.isBlank()) {
            InstitutionalLotationGovernanceEntry byId = lotacoes.stream().filter(item -> Objects.equals(item.lotationId(), lotationId)).findFirst().orElse(null);
            if (byId != null) {
                return byId;
            }
        }
        if (userId != null) {
            return lotacoes.stream().filter(item -> Objects.equals(item.userId(), userId)).findFirst().orElse(null);
        }
        return null;
    }

    private int indexOf(List<InstitutionalCoverageDelegationEntry> delegations, String delegationId) {
        for (int i = 0; i < delegations.size(); i++) {
            if (Objects.equals(delegations.get(i).delegationId(), delegationId)) {
                return i;
            }
        }
        return -1;
    }

    private String resolveMunicipality(List<InstitutionalManagedUnitEntry> units, String unitCode) {
        if (unitCode == null || units == null) {
            return null;
        }
        return units.stream().filter(item -> Objects.equals(item.unitCode(), unitCode)).map(InstitutionalManagedUnitEntry::municipalityCoverage).findFirst().orElse(null);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
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

    private List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    private List<String> merge(List<String> left, List<String> right) {
        ArrayList<String> out = new ArrayList<>();
        if (left != null) {
            out.addAll(left);
        }
        if (right != null) {
            out.addAll(right);
        }
        return sanitize(out);
    }
}
