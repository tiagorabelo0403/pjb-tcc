package com.tcc.pjb.backend.modules.custas.application;

import com.tcc.pjb.backend.modules.custas.domain.CustaIsencaoPolicy;
import com.tcc.pjb.backend.modules.custas.domain.IsencaoCustaResult;
import com.tcc.pjb.backend.modules.custas.domain.TipoCusta;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.springframework.stereotype.Service;

@Service
public class CustaIsencaoPorRitoPolicy implements CustaIsencaoPolicy {

    @Override
    public IsencaoCustaResult verificar(Processo processo, TipoCusta tipoCusta) {
        if (processo == null) {
            return IsencaoCustaResult.naoIsento();
        }
        if (processo.getRamoDireito() == RamoDireito.INFANCIA_JUVENTUDE) {
            return IsencaoCustaResult.isento("Lei 8.069/90 (ECA), art. 141, § 2º — gratuidade nas ações do Estatuto da Criança e do Adolescente.");
        }
        if (tipoCusta == null || !tipoCusta.aplicaAoAjuizamentoInicial()) {
            return IsencaoCustaResult.naoIsento();
        }
        RitoProcessual rito = processo.getRito();
        if (rito == null) {
            return IsencaoCustaResult.naoIsento();
        }
        return switch (rito) {
            case JUIZADO_ESPECIAL_CIVEL ->
                    IsencaoCustaResult.isento("Lei 9.099/95, art. 54, caput — isenção de custas em primeiro grau no Juizado Especial Cível.");
            case JUIZADO_ESPECIAL_FEDERAL, PREVIDENCIARIO_JEF ->
                    IsencaoCustaResult.isento("Lei 10.259/2001, art. 1º c/c Lei 9.099/95, art. 54 — isenção de custas em primeiro grau no Juizado Especial Federal.");
            case JUIZADO_ESPECIAL_FAZENDA_PUBLICA ->
                    IsencaoCustaResult.isento("Lei 12.153/2009, art. 27 c/c Lei 9.099/95, art. 54 — isenção de custas em primeiro grau no Juizado Especial da Fazenda Pública.");
            default -> IsencaoCustaResult.naoIsento();
        };
    }
}
