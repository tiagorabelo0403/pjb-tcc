package com.tcc.pjb.backend.service.institutional;

import com.tcc.pjb.backend.model.entity.enums.GrauSigilo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class InstituicaoVisibilidadePolicy {

    public enum Decisao {
        PODE_VER,
        PODE_MOVIMENTAR,
        PODE_ASSINAR,
        PODE_ACESSAR_SIGILOSO,
        REQUER_STEP_UP,
        NEGADO
    }

    public record Resultado(
            Decisao decisao,
            List<String> motivos,
            List<String> riscos,
            boolean stepUpNecessario,
            String explicacaoDetalhada
    ) {}

    private InstituicaoVisibilidadePolicy() {}

    public static Resultado avaliar(
            LotacaoInstitucional lotacao,
            InstituicaoJudicial instituicao,
            GrauSigilo sigiloProcesso,
            LocalDate referencia
    ) {
        List<String> motivos = new ArrayList<>();
        List<String> riscos = new ArrayList<>();

        if (!lotacao.estaAtiva(referencia)) {
            return new Resultado(Decisao.NEGADO,
                    List.of("Lotação inativa na data de referência"),
                    List.of(), false,
                    "O usuário não possui lotação ativa na data " + referencia);
        }

        if (!lotacao.instituicaoId().equals(instituicao.id())) {
            motivos.add("Lotação em instituição diferente do processo");
            riscos.add("Acesso cross-institucional requer verificação");
        } else {
            motivos.add("Lotação na mesma instituição do processo");
        }

        PapelInstitucional papel = lotacao.papel();
        motivos.add("Papel: " + papel.codigo());

        if (sigiloProcesso == GrauSigilo.ULTRASSECRETO && !papel.podeAcessarSigiloso()) {
            return new Resultado(Decisao.NEGADO,
                    motivos,
                    List.of("Processo ultrassecreto — acesso negado para este papel"),
                    false,
                    "Processo classificado como ultrassecreto. Papel " + papel.codigo() + " não autorizado.");
        }

        if (sigiloProcesso == GrauSigilo.SIGILOSO && !papel.podeAcessarSigiloso()) {
            return new Resultado(Decisao.REQUER_STEP_UP,
                    motivos,
                    List.of("Processo sigiloso — step-up de autenticação necessário"),
                    true,
                    "Processo sigiloso. Autenticação reforçada necessária para acesso.");
        }

        if (sigiloProcesso == GrauSigilo.SIGILOSO && papel.podeUsarBreakGlass()) {
            motivos.add("Acesso sigiloso autorizado via break-glass");
        }

        if (papel.podeMovimentar() && papel.podeAssinar()) {
            return new Resultado(Decisao.PODE_ASSINAR, motivos, riscos, false,
                    "Papel autorizado a visualizar, movimentar e assinar neste processo.");
        }

        if (papel.podeMovimentar()) {
            return new Resultado(Decisao.PODE_MOVIMENTAR, motivos, riscos, false,
                    "Papel autorizado a visualizar e movimentar neste processo.");
        }

        return new Resultado(Decisao.PODE_VER, motivos, riscos, false,
                "Papel autorizado apenas a visualizar este processo.");
    }
}
