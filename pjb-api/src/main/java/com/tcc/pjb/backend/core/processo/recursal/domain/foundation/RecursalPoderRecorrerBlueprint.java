package com.tcc.pjb.backend.core.processo.recursal.domain.foundation;

import java.util.List;

public final class RecursalPoderRecorrerBlueprint {

    private RecursalPoderRecorrerBlueprint() {
    }

    public static RecursalPoderRecorrerEvento resolve(boolean renunciaDireitoRecorrer,
                                                      boolean desistiuRecursoInterposto,
                                                      boolean aquiescenciaExpressaOuTacita) {
        if (renunciaDireitoRecorrer) {
            return RecursalPoderRecorrerEvento.RENUNCIA;
        }
        if (desistiuRecursoInterposto) {
            return RecursalPoderRecorrerEvento.DESISTENCIA;
        }
        if (aquiescenciaExpressaOuTacita) {
            return RecursalPoderRecorrerEvento.AQUIESCENCIA;
        }
        return RecursalPoderRecorrerEvento.NENHUM;
    }

    public static String messageFor(RecursalPoderRecorrerEvento evento) {
        return switch (evento) {
            case RENUNCIA -> "renúncia ao direito de recorrer bloqueia a abertura de nova trilha recursal";
            case DESISTENCIA -> "desistência de recurso já interposto extingue a continuidade útil da trilha recursal";
            case AQUIESCENCIA -> "aquiescência expressa ou tácita torna incompatível a continuidade do recurso";
            case NENHUM -> "sem bloqueio identificado do poder de recorrer";
        };
    }

    public static List<String> secoesMinimas() {
        return List.of(
                RecursalFormalSectionLabels.CABIMENTO,
                RecursalFormalSectionLabels.RENUNCIA_DIREITO_RECORRER,
                RecursalFormalSectionLabels.DESISTENCIA_RECURSO,
                RecursalFormalSectionLabels.AQUIESCENCIA_DECISAO
        );
    }
}
