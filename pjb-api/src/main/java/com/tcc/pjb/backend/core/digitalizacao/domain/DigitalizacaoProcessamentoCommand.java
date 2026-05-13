package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.util.List;

public record DigitalizacaoProcessamentoCommand(Long jobId,
                                                List<byte[]> imagensPaginas,
                                                String idioma) {
}
