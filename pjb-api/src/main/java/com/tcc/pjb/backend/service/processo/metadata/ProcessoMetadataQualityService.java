package com.tcc.pjb.backend.service.processo.metadata;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoMetadataQualityService {

    public record QualitySnapshot(
            UUID processoId,
            int nota,
            List<ProcessoMetadataIssueType> issues,
            boolean bloqueantePendente,
            String classificacao
    ) {}

    private final ProcessoMetadataCompletenessPolicy completenessPolicy;

    public ProcessoMetadataQualityService(ProcessoMetadataCompletenessPolicy completenessPolicy) {
        this.completenessPolicy = completenessPolicy;
    }

    public QualitySnapshot avaliar(UUID processoId,
            ProcessoMetadataCompletenessPolicy.CompletenessInput input) {
        List<ProcessoMetadataIssueType> issues = completenessPolicy.avaliar(input);
        int desconto = issues.stream().mapToInt(ProcessoMetadataIssueType::pontoDesconto).sum();
        int nota = Math.max(0, 100 - desconto);
        boolean bloqueante = issues.stream().anyMatch(ProcessoMetadataIssueType::bloqueante);
        String classificacao = nota >= 90 ? "EXCELENTE"
                : nota >= 70 ? "BOM"
                : nota >= 50 ? "REGULAR"
                : "CRITICO";
        return new QualitySnapshot(processoId, nota, issues, bloqueante, classificacao);
    }
}
