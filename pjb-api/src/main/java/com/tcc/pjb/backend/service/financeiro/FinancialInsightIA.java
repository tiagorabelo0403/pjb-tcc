package com.tcc.pjb.backend.service.financeiro;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.financeiro.engine.ResultadoFinanceiro;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FinancialInsightIA {

    public void analisar(ResultadoFinanceiro resultado, Processo processo) {
        if (resultado == null || processo == null) return;

        
        resultado.normalize();

        String procNum = processo.getNumeroUnificado();
        String desc = resultado.getDescricao();

        
        log.info("FIN_INSIGHT processoId={} procHash={} base={} total={} descHash={} descLen={}",
                processo.getId(),
                Hashes.sha256Hex(procNum),
                resultado.getBase(),
                resultado.getTotalEstimado(),
                Hashes.sha256Hex(desc),
                desc == null ? 0 : desc.length());
    }
}
