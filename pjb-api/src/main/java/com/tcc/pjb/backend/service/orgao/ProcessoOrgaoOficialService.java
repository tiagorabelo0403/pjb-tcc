package com.tcc.pjb.backend.service.orgao;

import java.util.EnumSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.orgao.OrgaoOficialTipo;
import com.tcc.pjb.backend.model.entity.orgao.ProcessoOrgaoOficial;
import com.tcc.pjb.backend.model.repository.ProcessoOrgaoOficialRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessoOrgaoOficialService {

    private static final EnumSet<OrgaoOficialTipo> DEFAULTS = EnumSet.of(
            OrgaoOficialTipo.PROCURADORIA_FEDERAL,
            OrgaoOficialTipo.PROCURADORIA_ESTADUAL,
            OrgaoOficialTipo.PROCURADORIA_MUNICIPAL
    );

    private final ProcessoOrgaoOficialRepository repository;
    private final AuditLedgerService auditLedgerService;

    @Transactional
    public void ensureDefaults(Processo processo) {
        if (processo == null || processo.getId() == null) return;

        boolean any = false;
        for (OrgaoOficialTipo t : DEFAULTS) {
            if (!repository.existsByProcesso_IdAndOrgaoTipo(processo.getId(), t)) {
                repository.save(ProcessoOrgaoOficial.builder()
                        .processo(processo)
                        .orgaoTipo(t)
                        .build());
                any = true;
            }
        }

        if (any) {
            
            auditLedgerService.appendSafely(
                    "ORGAOS_OFICIAIS_VINCULADOS",
                    "PROCESSO",
                    processo.getNumeroProcesso(),
                    null
            );
        }
    }
}
