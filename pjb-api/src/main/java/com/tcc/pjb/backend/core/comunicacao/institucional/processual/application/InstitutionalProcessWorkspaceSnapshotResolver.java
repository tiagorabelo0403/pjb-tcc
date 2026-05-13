package com.tcc.pjb.backend.core.comunicacao.institucional.processual.application;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

final class InstitutionalProcessWorkspaceSnapshotResolver {

    private final ProcessoRepository processoRepository;

    InstitutionalProcessWorkspaceSnapshotResolver(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    InstitutionalProcessWorkspaceSnapshot loadSnapshot(Long processoId,
                                         String rito,
                                         String fase,
                                         String status,
                                         String ramo) {
        if (processoId != null) {
            Optional<Processo> processo = processoRepository.findProcessoCompletoById(processoId);
            if (processo.isPresent()) {
                return snapshotFromEntity(processo.get());
            }
        }
        RitoProcessual ritoProcessual = RitoProcessual.tryParse(rito).orElse(null);
        FaseProcessual faseProcessual = fase == null || fase.isBlank() ? null : safeFase(fase);
        StatusProcesso statusProcesso = status == null || status.isBlank() ? null : StatusProcesso.fromString(status);
        RamoDireito ramoDireito = ramo == null || ramo.isBlank() ? inferRamo(ritoProcessual) : safeRamo(ramo, ritoProcessual);
        return new InstitutionalProcessWorkspaceSnapshot(ritoProcessual, faseProcessual, statusProcesso, ramoDireito,
                isUrgent(ritoProcessual, faseProcessual, statusProcesso, null, null),
                statusProcesso != null && statusProcesso.isRecursalOuEmbargos() || faseProcessual == FaseProcessual.RECURSAL,
                statusProcesso == StatusProcesso.EMBARGOS_DECLARACAO,
                faseProcessual != null && faseProcessual.isExecutionLike() || statusProcesso == StatusProcesso.CUMPRIMENTO_SENTENCA);
    }

    private InstitutionalProcessWorkspaceSnapshot snapshotFromEntity(Processo processo) {
        RitoProcessual rito = processo.getRito();
        FaseProcessual fase = processo.getFaseAtual();
        StatusProcesso status = processo.getStatusProcesso();
        return new InstitutionalProcessWorkspaceSnapshot(
                rito,
                fase,
                status,
                processo.getRamoDireito() == null ? inferRamo(rito) : processo.getRamoDireito(),
                isUrgent(rito, fase, status, processo.getAssunto(), processo.getClasseProcessual()),
                fase == FaseProcessual.RECURSAL || (status != null && status.isRecursalOuEmbargos()),
                status == StatusProcesso.EMBARGOS_DECLARACAO,
                (fase != null && fase.isExecutionLike()) || status == StatusProcesso.CUMPRIMENTO_SENTENCA
        );
    }

    private boolean isUrgent(RitoProcessual rito, FaseProcessual fase, StatusProcesso status, String assunto, String classe) {
        if (fase == FaseProcessual.AUDIENCIA_CUSTODIA) {
            return true;
        }
        if (rito != null && (rito == RitoProcessual.CIVIL_TUTELA_URGENTE || rito == RitoProcessual.CIVIL_TUTELA_CAUTELAR_ANTECEDENTE || rito == RitoProcessual.CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE)) {
            return true;
        }
        if (status != null && status == StatusProcesso.AUDIENCIA_DESIGNADA) {
            return true;
        }
        String text = normalize(assunto) + ' ' + normalize(classe);
        return text.contains("LIMINAR") || text.contains("URG") || text.contains("CUSTODIA") || text.contains("ALVARA") || text.contains("PLANTAO");
    }

    private FaseProcessual safeFase(String raw) {
        try {
            return FaseProcessual.valueOf(normalize(raw));
        } catch (Exception ignored) {
            return null;
        }
    }

    private RamoDireito safeRamo(String raw, RitoProcessual rito) {
        try {
            return RamoDireito.valueOf(normalize(raw));
        } catch (Exception ignored) {
            return inferRamo(rito);
        }
    }

    private RamoDireito inferRamo(RitoProcessual rito) {
        return rito == null ? null : rito.suggestedRamo();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
