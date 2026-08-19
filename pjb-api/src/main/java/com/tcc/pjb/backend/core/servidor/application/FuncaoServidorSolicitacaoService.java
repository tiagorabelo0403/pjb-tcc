package com.tcc.pjb.backend.core.servidor.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorSolicitacao;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import com.tcc.pjb.backend.model.repository.FuncaoServidorSolicitacaoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncaoServidorSolicitacaoService {

    private final FuncaoServidorSolicitacaoRepository solicitacaoRepository;
    private final FuncaoServidorJudiciarioRepository funcaoServidorJudiciarioRepository;
    private final FuncaoServidorDesignacaoService designacaoService;
    private final UsuarioRepository usuarioRepository;
    private final AuditLedgerService auditLedgerService;

    public FuncaoServidorSolicitacaoService(FuncaoServidorSolicitacaoRepository solicitacaoRepository,
                                             FuncaoServidorJudiciarioRepository funcaoServidorJudiciarioRepository,
                                             FuncaoServidorDesignacaoService designacaoService,
                                             UsuarioRepository usuarioRepository,
                                             AuditLedgerService auditLedgerService) {
        this.solicitacaoRepository = Objects.requireNonNull(solicitacaoRepository);
        this.funcaoServidorJudiciarioRepository = Objects.requireNonNull(funcaoServidorJudiciarioRepository);
        this.designacaoService = Objects.requireNonNull(designacaoService);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public FuncaoServidorSolicitacao solicitar(Long solicitanteId, Long unidadeId,
                                                FuncaoServidorJudiciario funcao, String motivo) {
        Usuario solicitante = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", solicitanteId));
        if (!solicitante.getTipoUsuario().isServidorJudiciario()) {
            throw new SecurityException("Apenas servidores judiciários podem solicitar designação institucional.");
        }
        FuncaoServidorSolicitacao solicitacao = new FuncaoServidorSolicitacao(solicitanteId, unidadeId, funcao, motivo);
        FuncaoServidorSolicitacao salva = solicitacaoRepository.save(solicitacao);
        auditLedgerService.appendSafely("FUNCAO_SERVIDOR_SOLICITACAO_CRIADA", "FUNCAO_SERVIDOR_SOLICITACAO",
                String.valueOf(salva.getId()));
        return salva;
    }

    @Transactional(readOnly = true)
    public List<FuncaoServidorSolicitacao> listarPorSolicitante(Long solicitanteId) {
        return solicitacaoRepository.findBySolicitanteIdOrderByRequestedAtDesc(solicitanteId);
    }

    @Transactional
    public FuncaoServidorSolicitacao aprovar(Long solicitacaoId, Long decisorId) {
        FuncaoServidorSolicitacao solicitacao = buscar(solicitacaoId);
        exigirPodeDecidir(decisorId, solicitacao.getUnidadeId(), solicitacao.getSolicitanteId());
        try {
            solicitacao.aprovar(decisorId);
        } catch (IllegalStateException e) {
            throw new RecursoJaExistenteException(e.getMessage());
        }
        FuncaoServidorSolicitacao salva = solicitacaoRepository.save(solicitacao);
        designacaoService.designarComLotacao(salva.getSolicitanteId(), salva.getUnidadeId(), salva.getFuncao(),
                LocalDate.now(), decisorId, null);
        auditLedgerService.appendSafely("FUNCAO_SERVIDOR_SOLICITACAO_APROVADA", "FUNCAO_SERVIDOR_SOLICITACAO",
                String.valueOf(salva.getId()));
        return salva;
    }

    @Transactional
    public FuncaoServidorSolicitacao rejeitar(Long solicitacaoId, Long decisorId, String motivo) {
        FuncaoServidorSolicitacao solicitacao = buscar(solicitacaoId);
        exigirPodeDecidir(decisorId, solicitacao.getUnidadeId(), solicitacao.getSolicitanteId());
        try {
            solicitacao.rejeitar(decisorId, motivo);
        } catch (IllegalStateException e) {
            throw new RecursoJaExistenteException(e.getMessage());
        }
        FuncaoServidorSolicitacao salva = solicitacaoRepository.save(solicitacao);
        auditLedgerService.appendSafely("FUNCAO_SERVIDOR_SOLICITACAO_REJEITADA", "FUNCAO_SERVIDOR_SOLICITACAO",
                String.valueOf(salva.getId()));
        return salva;
    }

    private FuncaoServidorSolicitacao buscar(Long solicitacaoId) {
        return solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("FuncaoServidorSolicitacao", solicitacaoId));
    }

    private void exigirPodeDecidir(Long decisorId, Long unidadeId, Long solicitanteId) {
        if (Objects.equals(decisorId, solicitanteId)) {
            throw new SecurityException("Não é permitido decidir a própria solicitação.");
        }
        Usuario decisor = usuarioRepository.findById(decisorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", decisorId));
        if (decisor.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return;
        }
        boolean diretorDaUnidade = funcaoServidorJudiciarioRepository
                .findByUsuarioIdAndUnidadeIdAndFuncaoAndAtivo(decisorId, unidadeId,
                        FuncaoServidorJudiciario.DIRETOR_SECRETARIA, true)
                .isPresent();
        if (!diretorDaUnidade) {
            throw new SecurityException("Somente ROLE_ADMINISTRADOR ou o Diretor de Secretaria da mesma unidade podem decidir esta solicitação.");
        }
    }
}
