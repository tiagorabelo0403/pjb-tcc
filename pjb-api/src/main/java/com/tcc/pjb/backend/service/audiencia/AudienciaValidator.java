package com.tcc.pjb.backend.service.audiencia;

import java.util.*;
import com.tcc.pjb.backend.model.entity.AudienciaContexto;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;

public class AudienciaValidator {

    
    
    
    private static final Map<TipoAudiencia, Set<EsferaJurisdicao>> COMPATIBILIDADE_ESFERA = Map.ofEntries(
            Map.entry(TipoAudiencia.AUDIENCIA_JURI, Set.of(EsferaJurisdicao.JUSTICA_ESTADUAL)),
            Map.entry(TipoAudiencia.AUDIENCIA_UNA_TRABALHO, Set.of(EsferaJurisdicao.JUSTICA_TRABALHO)),
            Map.entry(TipoAudiencia.AUDIENCIA_PUBLICA_AMBIENTAL, Set.of(EsferaJurisdicao.JUSTICA_ESTADUAL, EsferaJurisdicao.JUSTICA_FEDERAL)),
            Map.entry(TipoAudiencia.AUDIENCIA_CUSTODIA, Set.of(EsferaJurisdicao.JUSTICA_ESTADUAL, EsferaJurisdicao.JUSTICA_FEDERAL))
    );

    
    
    
    private static final Map<TipoAudiencia, Set<MateriaJurisdicao>> COMPATIBILIDADE_MATERIA = Map.ofEntries(
            Map.entry(TipoAudiencia.AUDIENCIA_JURI, Set.of(MateriaJurisdicao.PENAL)),
            Map.entry(TipoAudiencia.AUDIENCIA_UNA_TRABALHO, Set.of(MateriaJurisdicao.TRABALHISTA)),
            Map.entry(TipoAudiencia.AUDIENCIA_PUBLICA_AMBIENTAL, Set.of(MateriaJurisdicao.AMBIENTAL)),
            Map.entry(TipoAudiencia.AUDIENCIA_CUSTODIA, Set.of(MateriaJurisdicao.PENAL))
    );

    
    
    
    private static final Map<TipoAudiencia, String> FUNDAMENTACAO_LEGAL = Map.ofEntries(
            Map.entry(TipoAudiencia.AUDIENCIA_CONCILIACAO, "Art. 334, CPC/2015"),
            Map.entry(TipoAudiencia.AUDIENCIA_MEDIACAO, "Art. 165, CPC/2015"),
            Map.entry(TipoAudiencia.AUDIENCIA_INSTRUCAO_E_JULGAMENTO, "Arts. 357-366, CPC/2015"),
            Map.entry(TipoAudiencia.AUDIENCIA_JUSTIFICACAO, "Art. 300, CPC/2015"),
            Map.entry(TipoAudiencia.AUDIENCIA_CUSTODIA, "Art. 310, CPP"),
            Map.entry(TipoAudiencia.AUDIENCIA_INTERROGATORIO, "Art. 185, CPP"),
            Map.entry(TipoAudiencia.AUDIENCIA_JURI, "Arts. 406-497, CPP"),
            Map.entry(TipoAudiencia.AUDIENCIA_UNA_TRABALHO, "Art. 849, CLT"),
            Map.entry(TipoAudiencia.AUDIENCIA_PUBLICA_AMBIENTAL, "Lei 6.938/81 e Res. CONAMA 09/87")
    );

    
    
    
    public static boolean validar(AudienciaContexto contexto) {
        TipoAudiencia tipo = contexto.getTipo();
        EsferaJurisdicao esfera = contexto.getEsfera();
        MateriaJurisdicao materia = contexto.getMateria();

        
        if (COMPATIBILIDADE_ESFERA.containsKey(tipo)) {
            if (!COMPATIBILIDADE_ESFERA.get(tipo).contains(esfera)) {
                return false;
            }
        }

        
        if (COMPATIBILIDADE_MATERIA.containsKey(tipo)) {
            if (!COMPATIBILIDADE_MATERIA.get(tipo).contains(materia)) {
                return false;
            }
        }

        return true;
    }

    
    
    
    public static Optional<String> explicarErro(AudienciaContexto contexto) {
        if (validar(contexto)) return Optional.empty();

        StringBuilder sb = new StringBuilder("Incompatibilidade detectada: ");
        sb.append("Audiência ").append(contexto.getTipo());

        if (COMPATIBILIDADE_ESFERA.containsKey(contexto.getTipo()) &&
                !COMPATIBILIDADE_ESFERA.get(contexto.getTipo()).contains(contexto.getEsfera())) {
            sb.append(" não pode ocorrer na esfera ").append(contexto.getEsfera());
        }

        if (COMPATIBILIDADE_MATERIA.containsKey(contexto.getTipo()) &&
                !COMPATIBILIDADE_MATERIA.get(contexto.getTipo()).contains(contexto.getMateria())) {
            sb.append(" não é válida para a matéria ").append(contexto.getMateria());
        }

        return Optional.of(sb.toString());
    }

    
    
    
    public static String fundamentacao(TipoAudiencia tipo) {
        return FUNDAMENTACAO_LEGAL.getOrDefault(tipo, "Fundamentação não cadastrada");
    }
}