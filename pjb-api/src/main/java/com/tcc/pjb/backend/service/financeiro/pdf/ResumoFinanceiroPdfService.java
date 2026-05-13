package com.tcc.pjb.backend.service.financeiro.pdf;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.financeiro.engine.ResultadoFinanceiro;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ResumoFinanceiroPdfService {

    
    public void gerarResumoFinanceiro(Processo processo, ResultadoFinanceiro resultado) {
        if (processo == null || resultado == null) return;
        resultado.normalize();

        log.info("Resumo financeiro gerado | processo={} | total={} | componentes={} ",
                safe(processo.getNumeroUnificado()),
                resultado.getTotalEstimado(),
                resultado.getComponentesSafe());
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "(sem_numero)" : s;
    }
}
