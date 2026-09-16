package com.tcc.pjb.backend.core.servidor.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncaoServidorEncerramentoService {

    private static final Logger log = LoggerFactory.getLogger(FuncaoServidorEncerramentoService.class);

    private final FuncaoServidorApplicationService funcaoServidorApplicationService;
    private final LotacaoInstituicaoMaterializationService lotacaoInstituicaoMaterializationService;
    private final AuditLedgerService auditLedgerService;

    public FuncaoServidorEncerramentoService(FuncaoServidorApplicationService funcaoServidorApplicationService,
                                              LotacaoInstituicaoMaterializationService lotacaoInstituicaoMaterializationService,
                                              AuditLedgerService auditLedgerService) {
        this.funcaoServidorApplicationService = Objects.requireNonNull(funcaoServidorApplicationService);
        this.lotacaoInstituicaoMaterializationService = Objects.requireNonNull(lotacaoInstituicaoMaterializationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public FuncaoServidorJudiciarioEntity encerrarComLotacao(Long funcaoId, LocalDate dataFim, Long operadorId) {
        FuncaoServidorJudiciarioEntity entidade =
                funcaoServidorApplicationService.encerrar(funcaoId, dataFim, operadorId);
        auditLedgerService.appendSafely("FUNCAO_SERVIDOR_ENCERRADA", "FUNCAO_SERVIDOR_JUDICIARIO",
                String.valueOf(entidade.getId()));
        List<FuncaoServidorJudiciarioEntity> restantes = funcaoServidorApplicationService.funcoesAtivas(
                entidade.getUsuarioId(), entidade.getUnidadeId());
        if (restantes.isEmpty()) {
            try {
                lotacaoInstituicaoMaterializationService.encerrarLotacaoSeAtiva(
                        entidade.getUsuarioId(), entidade.getUnidadeId(), dataFim);
            } catch (RuntimeException e) {
                log.warn("Falha ao encerrar LotacaoInstituicao para usuarioId={}, unidadeId={}: {}",
                        entidade.getUsuarioId(), entidade.getUnidadeId(), e.getMessage(), e);
            }
        }
        return entidade;
    }
}
