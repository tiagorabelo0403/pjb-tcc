package com.tcc.pjb.backend.service.acaoconstitucional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HabeasDataReadinessService {

    public enum FinalidadeHD {
        CONHECER_INFORMACOES,
        RETIFICAR_DADOS,
        ANOTAR_INFORMACOES_CONTESTADAS
    }

    public record HabeasDataInput(
            String processoId,
            boolean solicitacaoAdministrativaPrevia,
            boolean respostaNegadaOuOmissao,
            boolean bancoPublicoOuCaraterPublico,
            FinalidadeHD finalidade
    ) {}

    public record HabeasDataResult(
            List<String> requisitosVerificados,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação judicial. Não substitui análise do magistrado.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir antes de submeter ao magistrado.";

    public HabeasDataResult avaliar(HabeasDataInput input) {
        List<String> verificados = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        if (!input.solicitacaoAdministrativaPrevia()) {
            pendencias.add("Possível requisito a conferir: prévio requerimento administrativo negado ou sem resposta (Lei 9.507/97 art. 2º — interesse de agir).");
        } else {
            verificados.add("Requerimento administrativo prévio realizado — conferido.");
        }
        if (!input.respostaNegadaOuOmissao()) {
            pendencias.add("Possível requisito a conferir: negativa ou omissão da entidade não identificada — interesse de agir a verificar.");
        } else {
            verificados.add("Negativa ou omissão da entidade — conferida.");
        }
        if (!input.bancoPublicoOuCaraterPublico()) {
            pendencias.add("Possível requisito a conferir: banco de dados deve ser governamental ou de caráter público (CF art. 5º LXXII).");
        } else {
            verificados.add("Banco de dados governamental ou de caráter público — conferido.");
        }
        if (input.finalidade() != null) {
            verificados.add("Finalidade declarada: " + descricaoFinalidade(input.finalidade()));
        }

        return new HabeasDataResult(List.copyOf(verificados), List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private String descricaoFinalidade(FinalidadeHD f) {
        return switch (f) {
            case CONHECER_INFORMACOES -> "conhecer informações pessoais (Lei 9.507/97 art. 7º I)";
            case RETIFICAR_DADOS -> "retificação de dados (Lei 9.507/97 art. 7º II)";
            case ANOTAR_INFORMACOES_CONTESTADAS -> "anotação de informações contestadas (Lei 9.507/97 art. 7º III)";
        };
    }
}
