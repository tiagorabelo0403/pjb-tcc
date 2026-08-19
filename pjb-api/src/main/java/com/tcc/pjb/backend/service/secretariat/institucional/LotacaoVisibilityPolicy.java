package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class LotacaoVisibilityPolicy implements UnidadeInstitucionalVisibilityPolicy {

    private final LotacaoInstituicaoRepository lotacaoRepository;

    public LotacaoVisibilityPolicy(LotacaoInstituicaoRepository lotacaoRepository) {
        this.lotacaoRepository = Objects.requireNonNull(lotacaoRepository);
    }

    @Override
    public boolean podeVer(Usuario usuario, UnidadeInstituicao unidade) {
        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return true;
        }
        return lotacaoRepository.findAtivasByUsuario(usuario).stream()
                .map(LotacaoInstituicao::getUnidade)
                .anyMatch(u -> u.getId().equals(unidade.getId()));
    }
}
