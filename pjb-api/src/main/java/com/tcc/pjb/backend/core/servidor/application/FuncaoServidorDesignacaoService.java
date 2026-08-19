package com.tcc.pjb.backend.core.servidor.application;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.LocalDate;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FuncaoServidorDesignacaoService {

    private static final Logger log = LoggerFactory.getLogger(FuncaoServidorDesignacaoService.class);

    private final FuncaoServidorApplicationService funcaoServidorApplicationService;
    private final UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LotacaoInstituicaoRepository lotacaoInstituicaoRepository;

    public FuncaoServidorDesignacaoService(FuncaoServidorApplicationService funcaoServidorApplicationService,
                                            UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository,
                                            UsuarioRepository usuarioRepository,
                                            LotacaoInstituicaoRepository lotacaoInstituicaoRepository) {
        this.funcaoServidorApplicationService = Objects.requireNonNull(funcaoServidorApplicationService);
        this.unidadeJudiciariaCompetenciaRepository = Objects.requireNonNull(unidadeJudiciariaCompetenciaRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.lotacaoInstituicaoRepository = Objects.requireNonNull(lotacaoInstituicaoRepository);
    }

    @Transactional
    public FuncaoServidorJudiciarioEntity designarComLotacao(Long usuarioId, Long unidadeId,
                                                              FuncaoServidorJudiciario funcao,
                                                              LocalDate dataInicio, Long designadoPorId,
                                                              String portaria) {
        FuncaoServidorJudiciarioEntity entidade = funcaoServidorApplicationService.designar(
                usuarioId, unidadeId, funcao, dataInicio, designadoPorId, portaria);
        try {
            materializarLotacaoSePonteExistir(usuarioId, unidadeId, funcao, dataInicio);
        } catch (RuntimeException e) {
            log.warn("Falha ao materializar LotacaoInstituicao para usuarioId={}, unidadeId={}, funcao={}: {}",
                    usuarioId, unidadeId, funcao, e.getMessage(), e);
        }
        return entidade;
    }

    private void materializarLotacaoSePonteExistir(Long usuarioId, Long unidadeId,
                                                     FuncaoServidorJudiciario funcao, LocalDate dataInicio) {
        UnidadeJudiciariaCompetencia unidadeCompetencia = unidadeJudiciariaCompetenciaRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UnidadeJudiciariaCompetencia", unidadeId));
        UnidadeInstituicao unidadeInstituicao = unidadeCompetencia.getUnidadeInstituicao();
        if (unidadeInstituicao == null) {
            return;
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));
        LotacaoInstituicao lotacao = lotacaoInstituicaoRepository
                .findFirstByUsuarioAndUnidadeOrderByInicioDesc(usuario, unidadeInstituicao)
                .orElseGet(LotacaoInstituicao::new);
        lotacao.setUsuario(usuario);
        lotacao.setUnidade(unidadeInstituicao);
        lotacao.setInicio(dataInicio);
        lotacao.setFim(null);
        lotacao.setPapelNaUnidade(funcao.label());
        lotacaoInstituicaoRepository.save(lotacao);
    }
}
