package com.tcc.pjb.backend.core.plataforma.substituicao.parity;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoSistemaLegado;

public record PjbLegacyParityFinding(PjbSubstituicaoSistemaLegado legacySystem,
                                     PjbLegacyParityCapability capability,
                                     boolean covered,
                                     String evidencePath) {
}
