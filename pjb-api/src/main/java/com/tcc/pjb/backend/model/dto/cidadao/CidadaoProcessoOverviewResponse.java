package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import com.tcc.pjb.backend.model.dto.processo.ProcessoAcessoVisibilidadeResponse;

public record CidadaoProcessoOverviewResponse(
        LocalDateTime generatedAt,
        Long processoId,
        CidadaoProcessoCardDto card,
        String uiLegendUrl,
        AreaLinks links,
        String instanciasUrl,
        String julgamentosUrl,
        ProcessoAcessoVisibilidadeResponse acompanhamentoAcessos
) {}
