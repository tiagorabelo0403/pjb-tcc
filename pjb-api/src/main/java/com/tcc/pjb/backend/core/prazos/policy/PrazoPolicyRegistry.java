package com.tcc.pjb.backend.core.prazos.policy;

import com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice.PjbRitoContext;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.springframework.stereotype.Service;
@Service
public class PrazoPolicyRegistry {

    public PrazoRegime defaultRegime(MateriaJurisdicao materia, RitoProcessual rito) {
        if (rito == RitoProcessual.EXECUCAO_PENAL
                || rito == RitoProcessual.PROCEDIMENTO_PENAL_COMUM
                || rito == RitoProcessual.PROCEDIMENTO_PENAL_SUMARIO
                || rito == RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO
                || rito == RitoProcessual.TRIBUNAL_JURI
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL
                || rito == RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR
                || rito == RitoProcessual.MILITAR_IPM
                || rito == RitoProcessual.ELEITORAL
                || rito.name().startsWith("ELEITORAL_")) {
            return PrazoRegime.CORRIDOS;
        }

        if (materia == null) {
            return PrazoRegime.UTEIS;
        }

        return switch (materia) {
            case PENAL, ELEITORAL, MILITAR -> PrazoRegime.CORRIDOS;
            default -> PrazoRegime.UTEIS;
        };
    }
    public PrazoRegime resolveByPartyProfile(RamoDireito ramo,
                                            RitoProcessual rito,
                                            boolean defensoria,
                                            boolean ministerioPublico,
                                            boolean fazenda) {
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            return PrazoRegime.ECA;
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            return PrazoRegime.CLT_HORAS_UTEIS;
        }
        if (defensoria || ministerioPublico || fazenda) {
            return PrazoRegime.DOBRO_UTEIS;
        }
        return defaultRegime(ramo == null ? null : materiaFromRamo(ramo), rito);
    }

    public PrazoRegime resolveByRitoContext(PjbRitoContext context) {
        if (context == null) return PrazoRegime.UTEIS;
        String policy = context.prazoPolicy();
        if (policy == null) return PrazoRegime.UTEIS;
        return switch (policy) {
            case "PRAZO_PENAL", "PRAZO_ELEITORAL", "PRAZO_MILITAR", "PRAZO_ELEITORAL_HORAS" -> PrazoRegime.CORRIDOS;
            case "PRAZO_JECRIM" -> PrazoRegime.HORAS;
            case "PRAZO_INFANCIA" -> PrazoRegime.ECA;
            case "PRAZO_TRABALHISTA" -> PrazoRegime.CLT_HORAS_UTEIS;
            case "PRAZO_JUIZADO_ESPECIAL", "PRAZO_JUIZADO_FAZENDA", "PRAZO_JUIZADO_ESPECIAL_CIVEL",
                 "PRAZO_PREVIDENCIARIO", "PRAZO_FAZENDA", "PRAZO_CIVIL_ORDINARIO",
                 "PRAZO_FALENCIA", "PRAZO_AGRARIO", "PRAZO_AMBIENTAL",
                 "PRAZO_ADMINISTRATIVO", "PRAZO_CONSENSUAL", "PRAZO_CONSTITUCIONAL",
                 "PRAZO_INTERNACIONAL", "PRAZO_FAMILIA" -> PrazoRegime.UTEIS;
            default -> PrazoRegime.UTEIS;
        };
    }

    private MateriaJurisdicao materiaFromRamo(RamoDireito ramo) {
        return switch (ramo) {
            case PENAL -> MateriaJurisdicao.PENAL;
            case ELEITORAL -> MateriaJurisdicao.ELEITORAL;
            case MILITAR -> MateriaJurisdicao.MILITAR;
            default -> null;
        };
    }

}
