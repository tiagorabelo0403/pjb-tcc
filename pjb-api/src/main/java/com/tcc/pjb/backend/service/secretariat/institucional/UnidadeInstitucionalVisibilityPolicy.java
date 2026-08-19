package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;

public interface UnidadeInstitucionalVisibilityPolicy {
    boolean podeVer(Usuario usuario, UnidadeInstituicao unidade);
}
