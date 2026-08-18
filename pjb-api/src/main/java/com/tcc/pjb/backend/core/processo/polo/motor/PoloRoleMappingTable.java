package com.tcc.pjb.backend.core.processo.polo.motor;

import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.Map;
import java.util.Optional;

final class PoloRoleMappingTable {

    record PoloMapping(TipoPolo tipoPolo, TipoParte tipoParte) {}

    private static final Map<String, PoloMapping> GERAL = Map.ofEntries(
            Map.entry("AUTOR",                 new PoloMapping(TipoPolo.ATIVO,   TipoParte.AUTOR)),
            Map.entry("REU",                   new PoloMapping(TipoPolo.PASSIVO, TipoParte.REU)),
            Map.entry("ACUSACAO",              new PoloMapping(TipoPolo.ATIVO,   TipoParte.ACUSACAO)),
            Map.entry("ACUSADO",               new PoloMapping(TipoPolo.PASSIVO, TipoParte.ACUSADO)),
            Map.entry("RECLAMANTE",            new PoloMapping(TipoPolo.ATIVO,   TipoParte.RECLAMANTE)),
            Map.entry("RECLAMADA",             new PoloMapping(TipoPolo.PASSIVO, TipoParte.RECLAMADA)),
            Map.entry("EMPREGADO_ESTAVEL",     new PoloMapping(TipoPolo.PASSIVO, TipoParte.EMPREGADO_ESTAVEL)),
            Map.entry("SUBSTITUTO_PROCESSUAL", new PoloMapping(TipoPolo.ATIVO,   TipoParte.SUBSTITUTO_PROCESSUAL)),
            Map.entry("SUSCITANTE",            new PoloMapping(TipoPolo.ATIVO,   TipoParte.SUSCITANTE)),
            Map.entry("SUSCITADO",             new PoloMapping(TipoPolo.PASSIVO, TipoParte.SUSCITADO)),
            Map.entry("IMPETRANTE",            new PoloMapping(TipoPolo.ATIVO,   TipoParte.IMPETRANTE)),
            Map.entry("IMPETRADO",             new PoloMapping(TipoPolo.PASSIVO, TipoParte.IMPETRADO)),
            Map.entry("REPRESENTANTE",         new PoloMapping(TipoPolo.ATIVO,   TipoParte.REPRESENTANTE)),
            Map.entry("REPRESENTADO",          new PoloMapping(TipoPolo.PASSIVO, TipoParte.REPRESENTADO)),
            Map.entry("IMPUGNANTE",            new PoloMapping(TipoPolo.ATIVO,   TipoParte.IMPUGNANTE)),
            Map.entry("IMPUGNADO",             new PoloMapping(TipoPolo.PASSIVO, TipoParte.IMPUGNADO)),
            Map.entry("INVESTIGADO",           new PoloMapping(TipoPolo.PASSIVO, TipoParte.INVESTIGADO)),
            Map.entry("REQUERENTE",            new PoloMapping(TipoPolo.ATIVO,   TipoParte.REQUERENTE)),
            Map.entry("REQUERIDO",             new PoloMapping(TipoPolo.PASSIVO, TipoParte.REQUERIDO)),
            Map.entry("AUTOR_POPULAR",         new PoloMapping(TipoPolo.ATIVO,   TipoParte.AUTOR_POPULAR)),
            Map.entry("MENOR",                 new PoloMapping(TipoPolo.PASSIVO, TipoParte.MENOR)),
            Map.entry("SEGURADO",              new PoloMapping(TipoPolo.ATIVO,   TipoParte.SEGURADO)),
            Map.entry("CANDIDATO",             new PoloMapping(TipoPolo.ATIVO,   TipoParte.CANDIDATO)),
            Map.entry("SERVIDOR",              new PoloMapping(TipoPolo.ATIVO,   TipoParte.SERVIDOR)),
            Map.entry("PRESTADOR_CONTAS",      new PoloMapping(TipoPolo.ATIVO,   TipoParte.PRESTADOR_CONTAS)),
            Map.entry("DEVEDOR",               new PoloMapping(TipoPolo.ATIVO,   TipoParte.DEVEDOR))
    );

    // EMPREGADOR é o único código com TipoPolo dependente do rito:
    //   TRABALHISTA_INQUERITO_FALTA_GRAVE → ATIVO (empregador é o peticionante)
    //   TRABALHISTA_ACAO_CUMPRIMENTO      → PASSIVO (empregador é o executado)
    private static final Map<String, Map<String, PoloMapping>> OVERRIDES = Map.of(
            RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE.name(),
                    Map.of("EMPREGADOR", new PoloMapping(TipoPolo.ATIVO,   TipoParte.EMPREGADOR)),
            RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO.name(),
                    Map.of("EMPREGADOR", new PoloMapping(TipoPolo.PASSIVO, TipoParte.EMPREGADOR))
    );

    private PoloRoleMappingTable() {}

    static Optional<PoloMapping> lookup(RitoProcessual rito, String code) {
        if (rito != null) {
            Map<String, PoloMapping> ritoOverride = OVERRIDES.get(rito.name());
            if (ritoOverride != null) {
                PoloMapping override = ritoOverride.get(code);
                if (override != null) {
                    return Optional.of(override);
                }
            }
        }
        return Optional.ofNullable(GERAL.get(code));
    }
}
