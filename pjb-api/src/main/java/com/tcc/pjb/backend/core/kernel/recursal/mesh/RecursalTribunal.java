package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.EnumSet;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.util.EnumText;
import com.tcc.pjb.backend.domain.enums.TipoJustica;

public enum RecursalTribunal {
    TJ("Tribunal de Justiça", TipoJustica.ESTADUAL, InstanceLevel.SECOND_INSTANCE),
    TRF("Tribunal Regional Federal", TipoJustica.FEDERAL, InstanceLevel.SECOND_INSTANCE),
    TRT("Tribunal Regional do Trabalho", TipoJustica.TRABALHO, InstanceLevel.SECOND_INSTANCE),
    TRE("Tribunal Regional Eleitoral", TipoJustica.ELEITORAL, InstanceLevel.SECOND_INSTANCE),
    STJ("Superior Tribunal de Justiça", TipoJustica.SUPERIOR, InstanceLevel.SUPERIOR),
    TST("Tribunal Superior do Trabalho", TipoJustica.TRABALHO, InstanceLevel.SUPERIOR),
    TSE("Tribunal Superior Eleitoral", TipoJustica.ELEITORAL, InstanceLevel.SUPERIOR),
    STM("Superior Tribunal Militar", TipoJustica.MILITAR_FEDERAL, InstanceLevel.SUPERIOR),
    TNU("Turma Nacional de Uniformização", TipoJustica.FEDERAL, InstanceLevel.SECOND_INSTANCE),
    STF("Supremo Tribunal Federal", TipoJustica.SUPERIOR, InstanceLevel.EXTRAORDINARY);

    private static final Map<String, RecursalTribunal> ALIASES = Map.ofEntries(
            Map.entry("TRIBUNAL_DE_JUSTICA", TJ),
            Map.entry("TRIBUNAL_REGIONAL_FEDERAL", TRF),
            Map.entry("TRIBUNAL_REGIONAL_DO_TRABALHO", TRT),
            Map.entry("TRIBUNAL_REGIONAL_ELEITORAL", TRE),
            Map.entry("SUPERIOR_TRIBUNAL_DE_JUSTICA", STJ),
            Map.entry("TRIBUNAL_SUPERIOR_DO_TRABALHO", TST),
            Map.entry("TRIBUNAL_SUPERIOR_ELEITORAL", TSE),
            Map.entry("SUPERIOR_TRIBUNAL_MILITAR", STM),
            Map.entry("TURMA_NACIONAL_DE_UNIFORMIZACAO", TNU),
            Map.entry("SUPREMO_TRIBUNAL_FEDERAL", STF)
    );

    private final String descricao;
    private final TipoJustica tipoJustica;
    private final InstanceLevel instanceLevel;

    RecursalTribunal(String descricao, TipoJustica tipoJustica, InstanceLevel instanceLevel) {
        this.descricao = descricao;
        this.tipoJustica = tipoJustica;
        this.instanceLevel = instanceLevel;
    }

    public String descricao() {
        return descricao;
    }

    public TipoJustica tipoJustica() {
        return tipoJustica;
    }

    public InstanceLevel instanceLevel() {
        return instanceLevel;
    }

    public boolean secondInstanceCourt() {
        return EnumSet.of(TJ, TRF, TRT, TRE, TNU).contains(this);
    }

    public boolean superiorCourt() {
        return EnumSet.of(STJ, TST, TSE, STM).contains(this);
    }

    public boolean constitutionalCourt() {
        return this == STF;
    }

    public static RecursalTribunal from(TipoJustica tipoJustica, String tribunalCodigo) {
        RecursalTribunal byString = fromString(tribunalCodigo);
        if (byString != null) {
            return byString;
        }
        if (tipoJustica == null) {
            return TJ;
        }
        return switch (tipoJustica) {
            case ESTADUAL -> TJ;
            case FEDERAL -> TRF;
            case TRABALHO -> TRT;
            case ELEITORAL -> TRE;
            case MILITAR_FEDERAL -> STM;
            case SUPERIOR -> STJ;
            default -> TJ;
        };
    }

    public static RecursalTribunal fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) {
            return null;
        }
        RecursalTribunal alias = ALIASES.get(token);
        if (alias != null) {
            return alias;
        }
        try {
            return RecursalTribunal.valueOf(token);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @JsonCreator
    public static RecursalTribunal jsonCreator(String raw) {
        return fromString(raw);
    }
}
