package com.tcc.pjb.backend.service.secretariat.institucional;

import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;

public record PoloInstitucionalComposicaoEvent(Long processoId, String comarca, TipoUnidadeInstitucional tipo) {
}
