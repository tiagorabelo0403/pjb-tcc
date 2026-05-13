package com.tcc.pjb.backend.service;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.Competencia;
import com.tcc.pjb.backend.model.entity.Especie;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Profile;
import com.tcc.pjb.backend.model.entity.enums.ModuloProcesso;
import com.tcc.pjb.backend.model.entity.enums.Ramo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProfileEngine {

    
    public Profile loadProfile(String moduloTexto, Jurisdicao jurisdicao, Competencia competencia) {

        ModuloProcesso modulo = parseModulo(moduloTexto);
        Ramo ramo = traduzirModuloParaRamo(modulo);

        Profile p = new Profile();
        p.setRamo(ramo);
        p.setCompetencia(competencia);
        p.setEspecie(parseEspecie(jurisdicao != null ? jurisdicao.getEspecie() : null));

        log.debug("ProfileEngine -> modulo={}, ramo={}, especie={}, competencia={}", modulo, ramo, p.getEspecie(), competencia);
        return p;
    }

    private static Especie parseEspecie(String raw) {
        if (raw == null || raw.isBlank()) {
            return Especie.COMUM;
        }
        String norm = raw.trim().toUpperCase();
        try {
            return Especie.valueOf(norm);
        } catch (IllegalArgumentException ignored) {
            
            if (norm.contains("FEDERAL")) return Especie.FEDERAL;
            if (norm.contains("ESPECIAL")) return Especie.ESPECIAL;
            if (norm.contains("SUMARIO")) return Especie.SUMARIO;
            return Especie.COMUM;
        }
    }

    private ModuloProcesso parseModulo(String texto) {
        return ModuloProcesso.fromString(texto);
    }

    private Ramo traduzirModuloParaRamo(ModuloProcesso modulo) {
        if (modulo == null) {
            return Ramo.OUTROS;
        }
        return switch (modulo) {
            case CIVIL, CONSUMIDOR, REGISTROS_PUBLICOS, FALENCIAS, FAZENDARIA -> Ramo.CIVIL;
            case FAMILIA -> Ramo.FAMILIA;
            case PENAL -> Ramo.PENAL;
            case TRABALHISTA -> Ramo.TRABALHISTA;
            case ELEITORAL -> Ramo.ELEITORAL;
            case MILITAR -> Ramo.MILITAR;
            case AMBIENTAL -> Ramo.AMBIENTAL;
            case TRIBUTARIO -> Ramo.TRIBUTARIO;
            case PREVIDENCIARIO -> Ramo.PREVIDENCIARIO;
            case INFANCIA_JUVENTUDE -> Ramo.INFANCIA_JUVENTUDE;
            default -> Ramo.OUTROS;
        };
    }
}
