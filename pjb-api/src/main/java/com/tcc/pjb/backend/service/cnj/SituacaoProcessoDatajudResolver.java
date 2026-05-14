package com.tcc.pjb.backend.service.cnj;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.springframework.stereotype.Service;

@Service
public final class SituacaoProcessoDatajudResolver {

    public record SituacaoDatajud(
            String codigo,
            String descricao,
            boolean encerrado
    ) {}

    public SituacaoDatajud resolver(FaseProcessual fase, RitoProcessual rito) {
        return switch (fase) {
            case AUTUACAO -> new SituacaoDatajud("12056", "Aguardando distribuição", false);
            case DISTRIBUICAO -> new SituacaoDatajud("12057", "Distribuído", false);
            case CITACAO -> new SituacaoDatajud("12058", "Aguardando citação", false);
            case RESPOSTA -> new SituacaoDatajud("12059", "Aguardando resposta", false);
            case SANEAMENTO -> new SituacaoDatajud("12060", "Em saneamento", false);
            case JULGAMENTO -> resolverJulgamento(rito);
            case ARQUIVAMENTO -> new SituacaoDatajud("22", "Arquivado", true);
            default -> new SituacaoDatajud("00", "Em tramitação", false);
        };
    }

    private SituacaoDatajud resolverJulgamento(RitoProcessual rito) {
        if (rito != null && rito.name().startsWith("RECURSAL")) {
            return new SituacaoDatajud("12070", "Em julgamento colegiado", false);
        }
        return new SituacaoDatajud("12065", "Em julgamento", false);
    }
}
