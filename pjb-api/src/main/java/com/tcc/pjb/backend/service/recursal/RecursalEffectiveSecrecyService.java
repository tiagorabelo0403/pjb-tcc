package com.tcc.pjb.backend.service.recursal;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseFile;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class RecursalEffectiveSecrecyService {

    private final ProcessoRepository processoRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseProceedingRepository proceedingRepository;

    public RecursalEffectiveSecrecyService(ProcessoRepository processoRepository,
                                          CaseFileRepository caseFileRepository,
                                          CaseProceedingRepository proceedingRepository) {
        this.processoRepository = processoRepository;
        this.caseFileRepository = caseFileRepository;
        this.proceedingRepository = proceedingRepository;
    }

    @Transactional(readOnly = true)
    public NivelSigilo effectiveSecrecyForProcesso(Long processoId) {
        return effectiveSecrecySnapshot(processoId).effectiveSecrecy();
    }

    @Transactional(readOnly = true)
    public SecrecySnapshot effectiveSecrecySnapshot(Long processoId) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

        NivelSigilo base = processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;
        CaseFile caseFile = resolveCaseFile(processoId);
        if (caseFile == null) {
            return new SecrecySnapshot(base, base, null, 0, base.exigeCredencial(), null);
        }

        String sourceToken = proceedingRepository.findMaxSecrecyTokenByCaseFileId(caseFile.getId());
        NivelSigilo graphMax = NivelSigilo.fromString(sourceToken);
        NivelSigilo effective = graphMax.getNivel() >= base.getNivel() ? graphMax : base;
        List<CaseProceeding> proceedings = proceedingRepository.findAllByCaseFileId(caseFile.getId());

        return new SecrecySnapshot(
                base,
                effective,
                caseFile.getId(),
                proceedings.size(),
                effective.exigeCredencial(),
                sourceToken
        );
    }

    @Transactional(readOnly = true)
    public boolean requiresCredentialEscalation(Long processoId) {
        SecrecySnapshot snapshot = effectiveSecrecySnapshot(processoId);
        return snapshot.effectiveSecrecy().getNivel() > snapshot.baseSecrecy().getNivel() && snapshot.credentialRequired();
    }

    private CaseFile resolveCaseFile(Long processoId) {
        CaseFile root = caseFileRepository.findByRootProcessoId(processoId).orElse(null);
        if (root != null) {
            return root;
        }
        CaseProceeding linked = proceedingRepository.findFirstByLinkedProcessoId(processoId).orElse(null);
        if (linked == null) {
            return null;
        }
        return caseFileRepository.findById(linked.getCaseFileId()).orElse(null);
    }

    public record SecrecySnapshot(
            NivelSigilo baseSecrecy,
            NivelSigilo effectiveSecrecy,
            Long caseFileId,
            int proceedingsCount,
            boolean credentialRequired,
            String sourceToken
    ) {

        public boolean escalated() {
            return effectiveSecrecy.getNivel() > baseSecrecy.getNivel();
        }
    }
}
