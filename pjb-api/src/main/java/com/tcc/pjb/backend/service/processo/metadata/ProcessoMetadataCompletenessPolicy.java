package com.tcc.pjb.backend.service.processo.metadata;

import java.util.ArrayList;
import java.util.List;

public final class ProcessoMetadataCompletenessPolicy {

    private ProcessoMetadataCompletenessPolicy() {}

    public record CompletenessInput(
            boolean temClasse,
            boolean temAssunto,
            boolean partesComDocumento,
            boolean ritoCompativel,
            boolean competenciaConfirmada,
            boolean valorCausaConsistente,
            boolean sigiloJustificado,
            boolean documentosTipadosTodos,
            boolean prazoComMarco
    ) {}

    public List<ProcessoMetadataIssueType> avaliar(CompletenessInput input) {
        List<ProcessoMetadataIssueType> issues = new ArrayList<>();
        if (!input.temClasse()) issues.add(ProcessoMetadataIssueType.CLASSE_AUSENTE);
        if (!input.temAssunto()) issues.add(ProcessoMetadataIssueType.ASSUNTO_AUSENTE);
        if (!input.partesComDocumento()) issues.add(ProcessoMetadataIssueType.PARTE_SEM_DOCUMENTO);
        if (!input.ritoCompativel()) issues.add(ProcessoMetadataIssueType.RITO_INCOMPATIVEL);
        if (!input.competenciaConfirmada()) issues.add(ProcessoMetadataIssueType.COMPETENCIA_DUVIDOSA);
        if (!input.valorCausaConsistente()) issues.add(ProcessoMetadataIssueType.VALOR_CAUSA_INCONSISTENTE);
        if (!input.sigiloJustificado()) issues.add(ProcessoMetadataIssueType.SIGILO_SEM_JUSTIFICATIVA);
        if (!input.documentosTipadosTodos()) issues.add(ProcessoMetadataIssueType.DOCUMENTO_SEM_TIPO);
        if (!input.prazoComMarco()) issues.add(ProcessoMetadataIssueType.PRAZO_SEM_MARCO);
        return issues;
    }
}
