package com.tcc.pjb.backend.service.processo.autuacao;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RetificacaoAutuacaoDiffService {

    public record DiffInput(
            UUID processoId,
            RitoProcessual ritoAntes,
            RitoProcessual ritoDepois,
            String classeAntes,
            String classeDepois,
            BigDecimal valorCausaAntes,
            BigDecimal valorCausaDepois
    ) {}

    public record DiffItem(
            String campo,
            String valorAntes,
            String valorDepois,
            String impactoJuridico,
            boolean critico
    ) {}

    public record DiffResultado(
            UUID processoId,
            List<DiffItem> diferencas,
            boolean exigeDecisaoJudicial,
            String resumo
    ) {}

    public DiffResultado calcular(DiffInput input) {
        List<DiffItem> diffs = new ArrayList<>();
        boolean judicial = false;

        if (input.ritoAntes() != input.ritoDepois()) {
            diffs.add(new DiffItem("rito",
                    input.ritoAntes().name(), input.ritoDepois().name(),
                    "Alteração de rito muda prazos, competência, audiência e fluxo de fases.", true));
            judicial = true;
        }
        if (!equals(input.classeAntes(), input.classeDepois())) {
            diffs.add(new DiffItem("classe",
                    input.classeAntes(), input.classeDepois(),
                    "Alteração de classe CNJ pode exigir redistribuição ou nova citação.", false));
        }
        if (input.valorCausaAntes() != null && input.valorCausaDepois() != null
                && input.valorCausaAntes().compareTo(input.valorCausaDepois()) != 0) {
            diffs.add(new DiffItem("valorCausa",
                    input.valorCausaAntes().toPlainString(),
                    input.valorCausaDepois().toPlainString(),
                    "Alteração de valor pode mudar competência (JEC) e custas.", false));
        }

        String resumo = diffs.isEmpty()
                ? "Nenhuma diferença detectada."
                : diffs.size() + " campo(s) alterado(s)" + (judicial ? " — exige aprovação judicial." : ".");

        return new DiffResultado(input.processoId(), diffs, judicial, resumo);
    }

    private boolean equals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
