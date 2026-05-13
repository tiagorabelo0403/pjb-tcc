package com.tcc.pjb.backend.core.lgpd;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProcessoSigiloRlsEnvelopeService {

    private final ProcessoRepository processoRepository;
    private final DataClassificationCatalog dataClassificationCatalog;

    public ProcessoSigiloRlsEnvelopeService(ProcessoRepository processoRepository,
                                            DataClassificationCatalog dataClassificationCatalog) {
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.dataClassificationCatalog = Objects.requireNonNull(dataClassificationCatalog, "dataClassificationCatalog");
    }

    public ProcessoSigiloRlsEnvelope materialize(Long processoId, String httpMethod) {
        if (processoId == null) {
            throw new IllegalArgumentException("processoId");
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        DataClassificationEntry entry = dataClassificationCatalog.requireByEntityClass(Processo.class);
        NivelSigilo nivelSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        String tribunal = normalize(firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), processo.getUf()));
        String unidade = normalize(processo.getUnidadeJudiciariaCodigo());
        boolean requiresStepUp = entry.judicialSecrecyAware() && nivelSigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel();
        boolean requiresQualifiedCertificate = entry.judicialSecrecyAware() && nivelSigilo.getNivel() >= NivelSigilo.SIGILO_N3.getNivel();
        boolean readOnlyRecommended = isSafeMethod(httpMethod) || nivelSigilo == NivelSigilo.SEGREDO_ESTADO;
        String rlsScopeKey = buildScopeKey(tribunal, unidade, nivelSigilo);
        ArrayList<String> findings = new ArrayList<>();
        if (entry.rlsRecommended() && unidade == null) {
            findings.add("PROCESSO_SIGILOSO_SEM_UNIDADE_MATERIALIZADA");
        }
        if (entry.rlsRecommended() && tribunal == null) {
            findings.add("PROCESSO_SIGILOSO_SEM_TRIBUNAL_MATERIALIZADO");
        }
        if (nivelSigilo != NivelSigilo.PUBLICO && rlsScopeKey == null) {
            findings.add("SCOPE_RLS_SIGILOSO_INCOMPLETO");
        }
        if (nivelSigilo == NivelSigilo.SEGREDO_ESTADO) {
            findings.add("FLOW_REQUER_TRILHA_INSTITUCIONAL_MAXIMA");
        }
        findings.sort(Comparator.naturalOrder());
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        entry.categories().forEach(category -> categories.add(category.name()));
        return new ProcessoSigiloRlsEnvelope(
                processoId,
                entry.tableName(),
                nivelSigilo.name(),
                nivelSigilo.name(),
                unidade,
                tribunal,
                rlsScopeKey,
                entry.judicialSecrecyAware(),
                entry.rlsRecommended(),
                requiresStepUp,
                requiresQualifiedCertificate,
                readOnlyRecommended,
                List.copyOf(categories),
                List.copyOf(findings),
                normalize(processo.getCompetenciaTerritorialModo()),
                normalize(processo.getComarca())
        );
    }

    private boolean isSafeMethod(String method) {
        if (method == null || method.isBlank()) {
            return true;
        }
        String normalized = method.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("GET") || normalized.equals("HEAD") || normalized.equals("OPTIONS");
    }

    private String buildScopeKey(String tribunal, String unidade, NivelSigilo nivelSigilo) {
        if (nivelSigilo == null || nivelSigilo == NivelSigilo.PUBLICO) {
            return firstNonBlank(tribunal, unidade);
        }
        if (tribunal == null && unidade == null) {
            return null;
        }
        if (tribunal == null) {
            return "PROC_SIGILO|" + unidade + "|" + nivelSigilo.name();
        }
        if (unidade == null) {
            return "PROC_SIGILO|" + tribunal + "|" + nivelSigilo.name();
        }
        return "PROC_SIGILO|" + tribunal + "|" + unidade + "|" + nivelSigilo.name();
    }

    private String normalize(String value) {
        String token = firstNonBlank(value);
        return token == null ? null : token.trim();
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
}
