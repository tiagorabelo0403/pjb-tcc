package com.tcc.pjb.backend.core.servidor.application;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LotacaoInstituicaoMaterializationService {

    private final UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LotacaoInstituicaoRepository lotacaoInstituicaoRepository;

    public LotacaoInstituicaoMaterializationService(
            UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository,
            UsuarioRepository usuarioRepository,
            LotacaoInstituicaoRepository lotacaoInstituicaoRepository) {
        this.unidadeJudiciariaCompetenciaRepository = Objects.requireNonNull(unidadeJudiciariaCompetenciaRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.lotacaoInstituicaoRepository = Objects.requireNonNull(lotacaoInstituicaoRepository);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void materializarLotacaoSePonteExistir(Long usuarioId, Long unidadeId,
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
