package com.tcc.pjb.backend.service.conciliation;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcordoHomologacaoPreparationService {

    public record HomologacaoInput(
            UUID processoId,
            RitoProcessual rito,
            String termoAssinadoPorTodas,
            boolean custosPagos,
            boolean menorEnvolvido,
            boolean mpIntervindoSeNecessario
    ) {}

    public record HomologacaoPreparada(
            UUID processoId,
            boolean pronta,
            List<String> pendencias,
            String minutaEstrutura,
            boolean exigeDecisaoJudicial
    ) {}

    public HomologacaoPreparada preparar(HomologacaoInput input) {
        List<String> pendencias = new ArrayList<>();
        if (input.termoAssinadoPorTodas() == null || input.termoAssinadoPorTodas().isBlank()) {
            pendencias.add("Termo de acordo não assinado por todas as partes e advogados.");
        }
        if (!input.custosPagos()) {
            pendencias.add("Custas processuais pendentes — verificar antes de homologar.");
        }
        if (input.menorEnvolvido() && !input.mpIntervindoSeNecessario()) {
            pendencias.add("Menor envolvido — verificar necessidade de intervenção do Ministério Público.");
        }

        boolean pronta = pendencias.isEmpty();
        String minuta = "Sentença homologatória — art. 487, III, b, CPC | Rito: " + input.rito().name()
                + " | Extinção com resolução de mérito — trânsito em julgado após prazo recursal.";

        return new HomologacaoPreparada(input.processoId(), pronta, pendencias, minuta, true);
    }
}
