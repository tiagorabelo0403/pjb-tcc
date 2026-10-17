package com.tcc.pjb.backend.service.processual.representacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;

@Service
public class PeticaoInicialSemAdvogadoCabimentoService {

    private static final Set<RitoProcessual> RITOS_LIBERADOS = Set.of(RitoProcessual.JUIZADO_ESPECIAL_CIVEL);
    private static final BigDecimal SALARIOS_MINIMOS_SEM_ADVOGADO_JEC = new BigDecimal("20");

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;
    private final PjbTimeService timeService;

    public PeticaoInicialSemAdvogadoCabimentoService(SalarioMinimoNacionalService salarioMinimoNacionalService,
                                                     PjbTimeService timeService) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
        this.timeService = Objects.requireNonNull(timeService);
    }

    public boolean admiteRito(RitoProcessual rito) {
        return rito != null && RITOS_LIBERADOS.contains(rito);
    }

    public void exigirCabimento(RitoProcessual rito, BigDecimal valorCausa) {
        if (!admiteRito(rito)) {
            throw new AccessDeniedPjbException("Sem advogado, a petição inicial só pode ser apresentada no Juizado Especial Cível, nas causas de até 20 salários mínimos (Lei 9.099/95, art. 9º).");
        }
        if (valorCausa == null || valorCausa.signum() <= 0) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO,
                    "Informe o valor da causa: no Juizado Especial Cível sem advogado ele não pode passar de 20 salários mínimos (Lei 9.099/95, art. 9º).");
        }
        LocalDate dataReferencia = LocalDate.ofInstant(timeService.nowUtc(), timeService.legalZone());
        BigDecimal limite = salarioMinimoNacionalService.multiplicar(SALARIOS_MINIMOS_SEM_ADVOGADO_JEC, dataReferencia);
        if (valorCausa.compareTo(limite) > 0) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO,
                    "O valor da causa passa de 20 salários mínimos (R$ " + limite.toPlainString() + "). Acima desse valor, a parte precisa de advogado (Lei 9.099/95, art. 9º).")
                    .addMetadado("limiteSemAdvogado", limite)
                    .addMetadado("salariosMinimos", SALARIOS_MINIMOS_SEM_ADVOGADO_JEC)
                    .addMetadado("dataReferencia", dataReferencia.toString());
        }
    }
}
