package com.tcc.pjb.backend.core.security.identity;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Resolve o contexto institucional pela lotação canônica ativa do usuário.
 * Múltiplas lotações exigem seleção explícita, sem escolha ou validação prévia de papel pelo backend.
 * Não verifica afiliação nem homologação institucional, dependentes da consolidação da ONDA 1.5-B.
 */
@Service
public class ContextoInstitucionalResolver {

    private final LotacaoInstituicaoRepository lotacaoInstituicaoRepository;

    public ContextoInstitucionalResolver(LotacaoInstituicaoRepository lotacaoInstituicaoRepository) {
        this.lotacaoInstituicaoRepository = Objects.requireNonNull(lotacaoInstituicaoRepository);
    }

    public ContextoResolucao resolver(Usuario usuario) {
        List<LotacaoInstituicao> lotacoesAtivas =
                lotacaoInstituicaoRepository.findAtivasByUsuario(Objects.requireNonNull(usuario));
        if (lotacoesAtivas.isEmpty()) {
            return new ContextoNegado(MotivoContexto.SEM_LOTACAO_ATIVA);
        }
        if (lotacoesAtivas.size() > 1) {
            return new PendenteSelecao(lotacoesAtivas);
        }
        LotacaoInstituicao lotacao = lotacoesAtivas.getFirst();
        String papelNaUnidade = lotacao.getPapelNaUnidade();
        if (papelNaUnidade == null || papelNaUnidade.isBlank()) {
            return new ContextoNegado(MotivoContexto.SEM_PAPEL_NA_UNIDADE);
        }
        return new ContextoResolvido(
                new ContextoInstitucional(lotacao.getUnidade(), papelNaUnidade));
    }
}
