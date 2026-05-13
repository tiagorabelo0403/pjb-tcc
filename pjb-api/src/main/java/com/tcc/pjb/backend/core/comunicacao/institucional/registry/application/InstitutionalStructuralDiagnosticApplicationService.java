package com.tcc.pjb.backend.core.comunicacao.institucional.registry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticReport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalStructuralDiagnosticApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;

    public InstitutionalStructuralDiagnosticApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                               InstitutionalNominationStateRepository nominationRepository) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
    }

    public InstitutionalStructuralDiagnosticReport diagnosticar(String affiliationId) {
        Instant now = Instant.now();
        List<InstitutionalAffiliation> affiliations = loadAffiliations(affiliationId).stream()
                .sorted(Comparator.comparing(InstitutionalAffiliation::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        List<InstitutionalNomination> nominations = loadNominations(affiliationId, affiliations).stream()
                .sorted(Comparator.comparing(InstitutionalNomination::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        Map<String, InstitutionalAffiliation> affiliationById = affiliations.stream()
                .collect(Collectors.toMap(InstitutionalAffiliation::affiliationId, Function.identity(), (left, right) -> right, LinkedHashMap::new));
        ArrayList<InstitutionalStructuralDiagnosticFinding> findings = new ArrayList<>();

        for (InstitutionalAffiliation affiliation : affiliations) {
            List<InstitutionalNomination> scoped = nominations.stream().filter(item -> item.affiliationId().equals(affiliation.affiliationId())).toList();
            List<InstitutionalNomination> active = scoped.stream().filter(item -> item.ativaEm(now)).toList();
            long activeAdministrators = active.stream().filter(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre()).count();
            LinkedHashSet<String> missing = new LinkedHashSet<>();
            if (isBlank(affiliation.orgaoNome())) missing.add("orgaoNome");
            if (isBlank(affiliation.orgaoSigla())) missing.add("orgaoSigla");
            if (isBlank(affiliation.unidadeCodigo())) missing.add("unidadeCodigo");
            if (isBlank(affiliation.unidadeNome())) missing.add("unidadeNome");
            if (isBlank(affiliation.esferaAdministrativa())) missing.add("esferaAdministrativa");
            if (affiliation.ramosMateriais() == null || affiliation.ramosMateriais().isEmpty()) missing.add("ramosMateriais");
            if (affiliation.abrangenciasTerritoriais() == null || affiliation.abrangenciasTerritoriais().isEmpty()) missing.add("abrangenciasTerritoriais");
            if (affiliation.canaisHabilitados() == null || affiliation.canaisHabilitados().isEmpty()) missing.add("canaisHabilitados");
            if (affiliation.politicaCiencia() == null || affiliation.politicaCiencia().isEmpty()) missing.add("politicaCiencia");
            if (affiliation.sla() == null || affiliation.sla().isEmpty()) missing.add("sla");
            if (affiliation.regrasFallback() == null || affiliation.regrasFallback().isEmpty()) missing.add("regrasFallback");
            if (affiliation.conveniosIntegracoes() == null || affiliation.conveniosIntegracoes().isEmpty()) missing.add("conveniosIntegracoes");
            if (!missing.isEmpty()) {
                findings.add(finding("CADASTRO_INSTITUCIONAL_INCOMPLETO", InstitutionalRiskSeverity.ALTA, true,
                        "AFFILIATION", affiliation.affiliationId(),
                        "Cadastro institucional sem todos os elementos nucleares exigidos pelo desenho orgão->unidade->caixa->capacidade.",
                        missing.stream().map(item -> "campo=" + item).toList()));
            }
            if (affiliation.ativa() && active.isEmpty()) {
                findings.add(finding("AFILIACAO_SEM_NOMEACAO_ATIVA", InstitutionalRiskSeverity.CRITICA, true,
                        "AFFILIATION", affiliation.affiliationId(),
                        "Afiliação homologada sem qualquer pessoa física válida vinculada ao órgão em atuação.",
                        List.of("orgaoSigla=" + affiliation.orgaoSigla(), "unidadeCodigo=" + affiliation.unidadeCodigo())));
            }
            if (affiliation.ativa() && active.stream().noneMatch(item -> item.caixaCodigo() != null && !item.caixaCodigo().isBlank())) {
                findings.add(finding("AFILIACAO_SEM_CAIXA_OPERACIONAL", InstitutionalRiskSeverity.CRITICA, true,
                        "AFFILIATION", affiliation.affiliationId(),
                        "Afiliação ativa sem caixa operacional materializada para entrega institucional.",
                        List.of("orgaoSigla=" + affiliation.orgaoSigla(), "unidadeCodigo=" + affiliation.unidadeCodigo())));
            }
            if (affiliation.ativa() && affiliation.requerDuplaAprovacaoAdministrador() && activeAdministrators < 2) {
                findings.add(finding("DUPLA_ADMINISTRACAO_NAO_SATISFEITA", InstitutionalRiskSeverity.CRITICA, true,
                        "AFFILIATION", affiliation.affiliationId(),
                        "Afiliação exige dupla administração homologada e não atingiu o mínimo de dois administradores ativos.",
                        List.of("administradoresAtivos=" + activeAdministrators)));
            }
        }

        for (InstitutionalNomination nomination : nominations) {
            InstitutionalAffiliation affiliation = affiliationById.get(nomination.affiliationId());
            if (affiliation == null) {
                findings.add(finding("NOMEACAO_ORFA", InstitutionalRiskSeverity.CRITICA, true,
                        "NOMINATION", nomination.nominationId(),
                        "Nomeação aponta para afiliação institucional inexistente.",
                        List.of("affiliationId=" + nomination.affiliationId(), "userId=" + nomination.nominatedUserId())));
                continue;
            }
            if (nomination.ativaEm(now) && !affiliation.ativa()) {
                findings.add(finding("NOMEACAO_ATIVA_EM_AFILIACAO_INATIVA", InstitutionalRiskSeverity.CRITICA, true,
                        "NOMINATION", nomination.nominationId(),
                        "Nomeação ativa vinculada a afiliação institucional inativa ou não homologada.",
                        List.of("affiliationId=" + nomination.affiliationId(), "statusAfiliacao=" + affiliation.status().name())));
            }
            if (nomination.ativaEm(now) && (nomination.capacidades() == null || nomination.capacidades().isEmpty())) {
                findings.add(finding("NOMEACAO_SEM_CAPACIDADE", InstitutionalRiskSeverity.ALTA, true,
                        "NOMINATION", nomination.nominationId(),
                        "Nomeação ativa sem capacidades operacionais atribuídas para caixa institucional.",
                        List.of("userId=" + nomination.nominatedUserId(), "caixaCodigo=" + nomination.caixaCodigo())));
            }
            if (nomination.ativaEm(now)
                    && affiliation.trustFloor() != null
                    && nomination.trustFloor() != null
                    && nomination.trustFloor().ordem() < affiliation.trustFloor().ordem()) {
                findings.add(finding("TRUST_FLOOR_INCONSISTENTE", InstitutionalRiskSeverity.ALTA, false,
                        "NOMINATION", nomination.nominationId(),
                        "Nomeação com piso de confiança inferior ao piso mínimo exigido pela afiliação institucional.",
                        List.of("nominationTrust=" + nomination.trustFloor().name(), "affiliationTrust=" + affiliation.trustFloor().name())));
            }
        }

        Map<Long, List<InstitutionalNomination>> activeByUser = nominations.stream()
                .filter(item -> item.ativaEm(now))
                .collect(Collectors.groupingBy(InstitutionalNomination::nominatedUserId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<InstitutionalNomination>> entry : activeByUser.entrySet()) {
            List<InstitutionalNomination> scoped = entry.getValue();
            Set<String> affiliationsActing = scoped.stream().map(InstitutionalNomination::affiliationId).collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> orgaos = scoped.stream().map(item -> {
                InstitutionalAffiliation affiliation = affiliationById.get(item.affiliationId());
                return affiliation == null ? item.affiliationId() : affiliation.orgaoSigla();
            }).collect(Collectors.toCollection(LinkedHashSet::new));
            long adminMasterCount = scoped.stream().filter(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre()).count();
            if (affiliationsActing.size() >= 5 || orgaos.size() >= 4) {
                findings.add(finding("CPF_VINCULADO_A_MULTIPLOS_ORGAOS", InstitutionalRiskSeverity.ALTA, false,
                        "USER", String.valueOf(entry.getKey()),
                        "Um único CPF aparece como operador ativo em volume elevado de afiliações institucionais distintas.",
                        List.of("afiliacoesAtivas=" + affiliationsActing.size(), "orgaosDistintos=" + orgaos.size())));
            }
            if (adminMasterCount >= 2 && orgaos.size() >= 2) {
                findings.add(finding("ADMIN_MASTER_MULTI_ORGAO", InstitutionalRiskSeverity.ALTA, false,
                        "USER", String.valueOf(entry.getKey()),
                        "O mesmo CPF acumula administração mestre em múltiplos órgãos, exigindo auditoria humana.",
                        List.of("administracoesMestre=" + adminMasterCount, "orgaosDistintos=" + orgaos.size())));
            }
            Map<String, Long> duplicates = scoped.stream().collect(Collectors.groupingBy(item -> item.affiliationId() + "|" + item.unidadeCodigo() + "|" + item.caixaCodigo() + "|" + item.nominationRole(), LinkedHashMap::new, Collectors.counting()));
            duplicates.forEach((key, count) -> {
                if (count > 1) {
                    findings.add(finding("NOMEACAO_DUPLICADA", InstitutionalRiskSeverity.MEDIA, false,
                            "USER", String.valueOf(entry.getKey()),
                            "O mesmo usuário possui nomeações operacionais duplicadas para o mesmo órgão/unidade/caixa/papel.",
                            List.of("duplicidade=" + key, "quantidade=" + count)));
                }
            });
        }

        findings.sort(Comparator.comparing((InstitutionalStructuralDiagnosticFinding item) -> item.severity().weight()).reversed()
                .thenComparing(InstitutionalStructuralDiagnosticFinding::targetType)
                .thenComparing(InstitutionalStructuralDiagnosticFinding::targetId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(InstitutionalStructuralDiagnosticFinding::code));
        long blocking = findings.stream().filter(InstitutionalStructuralDiagnosticFinding::blocking).count();
        List<String> fundamentos = List.of(
                "scanner_estrutural_institucional",
                "regras_texto_cadastro_estrutura_vinculo_contexto",
                "afiliacoes_varridas=" + affiliations.size(),
                "nomeacoes_varridas=" + nominations.size(),
                "usuarios_ativos_varridos=" + activeByUser.size());
        return new InstitutionalStructuralDiagnosticReport(
                affiliationId,
                blocking == 0,
                findings.size(),
                blocking,
                List.copyOf(findings),
                fundamentos,
                now
        );
    }

    private List<InstitutionalAffiliation> loadAffiliations(String affiliationId) {
        if (affiliationId != null && !affiliationId.isBlank()) {
            return affiliationRepository.findByAffiliationId(affiliationId).stream().toList();
        }
        return affiliationRepository.findAll();
    }

    private List<InstitutionalNomination> loadNominations(String affiliationId, List<InstitutionalAffiliation> affiliations) {
        if (affiliationId != null && !affiliationId.isBlank()) {
            return nominationRepository.findByAffiliationId(affiliationId);
        }
        return nominationRepository.findByAffiliationIds(affiliations.stream()
                .map(InstitutionalAffiliation::affiliationId)
                .toList());
    }

    private InstitutionalStructuralDiagnosticFinding finding(String code,
                                                             InstitutionalRiskSeverity severity,
                                                             boolean blocking,
                                                             String targetType,
                                                             String targetId,
                                                             String message,
                                                             List<String> evidences) {
        return new InstitutionalStructuralDiagnosticFinding(code, severity, blocking, targetType, targetId, message, evidences);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
