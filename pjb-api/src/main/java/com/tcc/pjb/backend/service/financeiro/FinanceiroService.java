package com.tcc.pjb.backend.service.financeiro;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.financeiro.engine.*;
import com.tcc.pjb.backend.service.financeiro.pdf.ResumoFinanceiroPdfService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

    private final CivilFinanceEngine civilEngine;
    private final PenalFinanceEngine penalEngine;
    private final MilitarFinanceEngine militarEngine;
    private final EleitoralFinanceEngine eleitoralEngine;
    private final AdministrativoFinanceEngine administrativoEngine;
    private final TributarioFinanceEngine tributarioEngine;
    private final TrabalhistaFinanceEngine trabalhistaEngine;

    private final FinancialInsightIA insightIA;
    private final ResumoFinanceiroPdfService pdfService;

    
    public void processarFinanceiro(Processo processo) {
        if (processo == null) {
            throw new IllegalArgumentException("Processo não informado");
        }

        MateriaJurisdicao materia = processo.getMateria();
        if (materia == null) {
            throw new IllegalStateException("Matéria do processo não definida");
        }

        ResultadoFinanceiro resultado;

        switch (materia) {
            case CIVIL -> resultado = civilEngine.calcular(processo);
            case PENAL -> resultado = penalEngine.calcular(processo);
            case MILITAR -> resultado = militarEngine.calcular(processo);
            case ELEITORAL -> resultado = eleitoralEngine.calcular(processo);
            case ADMINISTRATIVO -> resultado = administrativoEngine.calcular(processo);
            case TRIBUTARIA -> resultado = tributarioEngine.calcular(processo);
            case TRABALHISTA -> resultado = trabalhistaEngine.calcular(processo);
            default -> throw new IllegalStateException("Matéria não suportada para financeiro: " + materia);
        }

        
        insightIA.analisar(resultado, processo);
        pdfService.gerarResumoFinanceiro(processo, resultado);
    }
}
