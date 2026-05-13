package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum RecursalTribunalDetalhado {
    TJAC(RecursalTribunal.TJ, "Tribunal de Justiça do Acre"),
    TJAL(RecursalTribunal.TJ, "Tribunal de Justiça de Alagoas"),
    TJAP(RecursalTribunal.TJ, "Tribunal de Justiça do Amapá"),
    TJAM(RecursalTribunal.TJ, "Tribunal de Justiça do Amazonas"),
    TJBA(RecursalTribunal.TJ, "Tribunal de Justiça da Bahia"),
    TJCE(RecursalTribunal.TJ, "Tribunal de Justiça do Ceará"),
    TJDFT(RecursalTribunal.TJ, "Tribunal de Justiça do Distrito Federal e Territórios"),
    TJES(RecursalTribunal.TJ, "Tribunal de Justiça do Espírito Santo"),
    TJGO(RecursalTribunal.TJ, "Tribunal de Justiça de Goiás"),
    TJMA(RecursalTribunal.TJ, "Tribunal de Justiça do Maranhão"),
    TJMT(RecursalTribunal.TJ, "Tribunal de Justiça de Mato Grosso"),
    TJMS(RecursalTribunal.TJ, "Tribunal de Justiça de Mato Grosso do Sul"),
    TJMG(RecursalTribunal.TJ, "Tribunal de Justiça de Minas Gerais"),
    TJPA(RecursalTribunal.TJ, "Tribunal de Justiça do Pará"),
    TJPB(RecursalTribunal.TJ, "Tribunal de Justiça da Paraíba"),
    TJPR(RecursalTribunal.TJ, "Tribunal de Justiça do Paraná"),
    TJPE(RecursalTribunal.TJ, "Tribunal de Justiça de Pernambuco"),
    TJPI(RecursalTribunal.TJ, "Tribunal de Justiça do Piauí"),
    TJRJ(RecursalTribunal.TJ, "Tribunal de Justiça do Rio de Janeiro"),
    TJRN(RecursalTribunal.TJ, "Tribunal de Justiça do Rio Grande do Norte"),
    TJRS(RecursalTribunal.TJ, "Tribunal de Justiça do Rio Grande do Sul"),
    TJRO(RecursalTribunal.TJ, "Tribunal de Justiça de Rondônia"),
    TJRR(RecursalTribunal.TJ, "Tribunal de Justiça de Roraima"),
    TJSC(RecursalTribunal.TJ, "Tribunal de Justiça de Santa Catarina"),
    TJSP(RecursalTribunal.TJ, "Tribunal de Justiça de São Paulo"),
    TJSE(RecursalTribunal.TJ, "Tribunal de Justiça de Sergipe"),
    TJTO(RecursalTribunal.TJ, "Tribunal de Justiça do Tocantins"),
    TRF1(RecursalTribunal.TRF, "Tribunal Regional Federal da 1ª Região"),
    TRF2(RecursalTribunal.TRF, "Tribunal Regional Federal da 2ª Região"),
    TRF3(RecursalTribunal.TRF, "Tribunal Regional Federal da 3ª Região"),
    TRF4(RecursalTribunal.TRF, "Tribunal Regional Federal da 4ª Região"),
    TRF5(RecursalTribunal.TRF, "Tribunal Regional Federal da 5ª Região"),
    TRF6(RecursalTribunal.TRF, "Tribunal Regional Federal da 6ª Região"),
    TRT1(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 1ª Região"),
    TRT2(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 2ª Região"),
    TRT3(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 3ª Região"),
    TRT4(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 4ª Região"),
    TRT5(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 5ª Região"),
    TRT6(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 6ª Região"),
    TRT7(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 7ª Região"),
    TRT8(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 8ª Região"),
    TRT9(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 9ª Região"),
    TRT10(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 10ª Região"),
    TRT11(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 11ª Região"),
    TRT12(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 12ª Região"),
    TRT13(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 13ª Região"),
    TRT14(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 14ª Região"),
    TRT15(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 15ª Região"),
    TRT16(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 16ª Região"),
    TRT17(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 17ª Região"),
    TRT18(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 18ª Região"),
    TRT19(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 19ª Região"),
    TRT20(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 20ª Região"),
    TRT21(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 21ª Região"),
    TRT22(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 22ª Região"),
    TRT23(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 23ª Região"),
    TRT24(RecursalTribunal.TRT, "Tribunal Regional do Trabalho da 24ª Região"),
    TREAC(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Acre"),
    TREAL(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Alagoas"),
    TREAP(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Amapá"),
    TREAM(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Amazonas"),
    TREBA(RecursalTribunal.TRE, "Tribunal Regional Eleitoral da Bahia"),
    TRECE(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Ceará"),
    TREDF(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Distrito Federal"),
    TREES(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Espírito Santo"),
    TREGO(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Goiás"),
    TREMA(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Maranhão"),
    TREMT(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Mato Grosso"),
    TREMS(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Mato Grosso do Sul"),
    TREMG(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Minas Gerais"),
    TREPA(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Pará"),
    TREPB(RecursalTribunal.TRE, "Tribunal Regional Eleitoral da Paraíba"),
    TREPR(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Paraná"),
    TREPE(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Pernambuco"),
    TREPI(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Piauí"),
    TRERJ(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Rio de Janeiro"),
    TRERN(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Rio Grande do Norte"),
    TRERS(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Rio Grande do Sul"),
    TRERO(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Rondônia"),
    TRERR(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Roraima"),
    TRESC(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Santa Catarina"),
    TRESP(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de São Paulo"),
    TRESE(RecursalTribunal.TRE, "Tribunal Regional Eleitoral de Sergipe"),
    TRETO(RecursalTribunal.TRE, "Tribunal Regional Eleitoral do Tocantins"),
    STJ(RecursalTribunal.STJ, "Superior Tribunal de Justiça"),
    TST(RecursalTribunal.TST, "Tribunal Superior do Trabalho"),
    TSE(RecursalTribunal.TSE, "Tribunal Superior Eleitoral"),
    STM(RecursalTribunal.STM, "Superior Tribunal Militar"),
    TNU(RecursalTribunal.TNU, "Turma Nacional de Uniformização"),
    STF(RecursalTribunal.STF, "Supremo Tribunal Federal");

    private static final Map<RecursalTribunal, RecursalTribunalDetalhado> DEFAULTS = Map.ofEntries(
            Map.entry(RecursalTribunal.TJ, TJSP),
            Map.entry(RecursalTribunal.TRF, TRF1),
            Map.entry(RecursalTribunal.TRT, TRT1),
            Map.entry(RecursalTribunal.TRE, TREDF),
            Map.entry(RecursalTribunal.STJ, STJ),
            Map.entry(RecursalTribunal.TST, TST),
            Map.entry(RecursalTribunal.TSE, TSE),
            Map.entry(RecursalTribunal.STM, STM),
            Map.entry(RecursalTribunal.TNU, TNU),
            Map.entry(RecursalTribunal.STF, STF)
    );

    private final RecursalTribunal familia;
    private final String descricao;

    RecursalTribunalDetalhado(RecursalTribunal familia, String descricao) {
        this.familia = familia;
        this.descricao = descricao;
    }

    public RecursalTribunal familia() {
        return familia;
    }

    public String descricao() {
        return descricao;
    }

    public RecursalTribunal tribunal() {
        return familia();
    }


    public static RecursalTribunalDetalhado fromFamily(RecursalTribunal familia) {
        return familia == null ? TJSP : DEFAULTS.getOrDefault(familia, TJSP);
    }

    public static RecursalTribunalDetalhado fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) {
            return null;
        }
        try {
            return RecursalTribunalDetalhado.valueOf(token);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @JsonCreator
    public static RecursalTribunalDetalhado jsonCreator(String raw) {
        return fromString(raw);
    }
}
