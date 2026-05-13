package com.tcc.pjb.backend.model.dto.consultapublica;

import com.tcc.pjb.backend.model.dto.publico.PublicMovimentacaoDTO;
import com.tcc.pjb.backend.model.dto.publico.SigiloUiDTO;
import java.time.LocalDateTime;
import java.util.List;

public record ConsultaPublicaPersonalProcessCardDto(
        Long processoId,
        String numero,
        String tribunal,
        String uf,
        String comarca,
        String vara,
        String tipoJustica,
        String ramoDireito,
        String ramoDireitoLabel,
        String ritoProcessual,
        String faseAtual,
        String statusProcesso,
        String classeProcessual,
        String assunto,
        LocalDateTime dataDistribuicao,
        LocalDateTime dataUltimaMovimentacao,
        SigiloUiDTO sigilo,
        String colorBand,
        PublicMovimentacaoDTO ultimaMovimentacao,
        ConsultaPublicaPersonalDeadlineDto proximoPrazo,
        List<ConsultaPublicaPersonalProcessTagDto> etiquetas,
        long totalDocumentos,
        String orientacaoLeitura,
        String detalheRoute,
        String prazosRoute,
        List<ConsultaPublicaWorkspaceActionDto> actions
) {
}
