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
            boolean cabivel,
            List<String> requisitosAtendidos,
            List<String> impeditivos
    ) {}

    public HabeasDataResult avaliar(HabeasDataInput input) {
        List<String> atendidos = new ArrayList<>();
        List<String> impeditivos = new ArrayList<>();

        if (!input.solicitacaoAdministrativaPrevia()) {
            impeditivos.add("HD exige prévio requerimento administrativo negado ou sem resposta (Lei 9.507/97 art. 2º — interesse de agir).");
        } else {
            atendidos.add("Requerimento administrativo prévio realizado.");
        }
        if (!input.respostaNegadaOuOmissao()) {
            impeditivos.add("Sem negativa ou omissão — interesse de agir não configurado.");
        } else {
            atendidos.add("Negativa ou omissão da entidade configurada.");
        }
        if (!input.bancoPublicoOuCaraterPublico()) {
            impeditivos.add("HD restrito a registros de entidades governamentais ou de caráter público (CF art. 5º LXXII).");
        } else {
            atendidos.add("Banco de dados é governamental ou de caráter público.");
        }
        if (input.finalidade() != null) {
            atendidos.add("Finalidade: " + descricaoFinalidade(input.finalidade()));
        }

        return new HabeasDataResult(impeditivos.isEmpty(), atendidos, impeditivos);
    }

    private String descricaoFinalidade(FinalidadeHD f) {
        return switch (f) {
            case CONHECER_INFORMACOES -> "conhecer informações pessoais (Lei 9.507/97 art. 7º I)";
            case RETIFICAR_DADOS -> "retificação de dados (Lei 9.507/97 art. 7º II)";
            case ANOTAR_INFORMACOES_CONTESTADAS -> "anotação de informações contestadas (Lei 9.507/97 art. 7º III)";
        };
    }
}
