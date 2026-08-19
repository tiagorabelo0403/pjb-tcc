package com.tcc.pjb.backend.core.servidor.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import java.time.LocalDate;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncaoServidorDesignacaoService {

    private static final Logger log = LoggerFactory.getLogger(FuncaoServidorDesignacaoService.class);

    private final FuncaoServidorApplicationService funcaoServidorApplicationService;
    private final LotacaoInstituicaoMaterializationService lotacaoInstituicaoMaterializationService;
    private final AuditLedgerService auditLedgerService;

    public FuncaoServidorDesignacaoService(FuncaoServidorApplicationService funcaoServidorApplicationService,
                                            LotacaoInstituicaoMaterializationService lotacaoInstituicaoMaterializationService,
                                            AuditLedgerService auditLedgerService) {
        this.funcaoServidorApplicationService = Objects.requireNonNull(funcaoServidorApplicationService);
        this.lotacaoInstituicaoMaterializationService = Objects.requireNonNull(lotacaoInstituicaoMaterializationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public FuncaoServidorJudiciarioEntity designarComLotacao(Long usuarioId, Long unidadeId,
                                                              FuncaoServidorJudiciario funcao,
                                                              LocalDate dataInicio, Long designadoPorId,
                                                              String portaria) {
        FuncaoServidorJudiciarioEntity entidade;
        try {
            entidade = funcaoServidorApplicationService.designar(
                    usuarioId, unidadeId, funcao, dataInicio, designadoPorId, portaria);
        } catch (DataIntegrityViolationException e) {
            throw new RecursoJaExistenteException(
                    "Já existe função ativa para o usuário na unidade: " + funcao);
        }
        auditLedgerService.appendSafely("FUNCAO_SERVIDOR_DESIGNADA", "FUNCAO_SERVIDOR_JUDICIARIO",
                String.valueOf(entidade.getId()));
        try {
            lotacaoInstituicaoMaterializationService.materializarLotacaoSePonteExistir(
                    usuarioId, unidadeId, funcao, dataInicio);
        } catch (RuntimeException e) {
            log.warn("Falha ao materializar LotacaoInstituicao para usuarioId={}, unidadeId={}, funcao={}: {}",
                    usuarioId, unidadeId, funcao, e.getMessage(), e);
        }
        return entidade;
    }
}
