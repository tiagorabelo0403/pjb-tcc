package com.tcc.pjb.backend.service.processual.nulidade;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NulidadeProcessualRiskService {

    public record DiagnosticoNulidade(
            UUID processoId,
            List<NulidadeProcessualRiskPolicy.RiscoNulidade> riscos,
            boolean bloqueioRecomendado,
            String mensagem
    ) {}

    private final NulidadeProcessualRiskPolicy policy;

    public NulidadeProcessualRiskService(NulidadeProcessualRiskPolicy policy) {
        this.policy = policy;
    }

    public DiagnosticoNulidade diagnosticar(UUID processoId,
            NulidadeProcessualRiskPolicy.NulidadeInput input) {
        List<NulidadeProcessualRiskPolicy.RiscoNulidade> riscos = policy.avaliar(input);
        boolean critico = riscos.stream().anyMatch(NulidadeProcessualRiskPolicy.RiscoNulidade::critico);
        String mensagem = riscos.isEmpty()
                ? "Nenhum risco de nulidade identificado antes desta movimentação."
                : riscos.size() + " risco(s) de nulidade — verificar antes de prosseguir.";
        return new DiagnosticoNulidade(processoId, riscos, critico, mensagem);
    }
}
