package com.tcc.pjb.backend.service.previdenciario;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BeneficioIncapacidadeChecklistService {

    public enum QualidadeSeguradoraStatus {
        ATIVO,
        PERIODO_GRACA,
        PERDIDA
    }

    public enum TipoIncapacidadeIndicada {
        TEMPORARIA,
        PERMANENTE,
        NAO_DETERMINADA
    }

    public record BeneficioIncapacidadeInput(
            String cpf,
            int contribuicoesTotais,
            QualidadeSeguradoraStatus qualidadeSeguradora,
            int mesesDesdeUltimaContribuicao,
            boolean acidenteDeTrabalho,
            boolean doencaProfissionalOuDoTrabalho,
            boolean catastrofeOuCrime,
            boolean laudoPericialApresentado,
            boolean incapacidadePermanenteIndicadaEmLaudo,
            boolean internacaoHospitalar,
            LocalDate dataInicioIncapacidade,
            LocalDate dataUltimaContribuicao
    ) {}

    public record BeneficioIncapacidadeResult(
            TipoIncapacidadeIndicada tipoIndicado,
            List<String> requisitosVerificados,
            List<String> pendenciasIdentificadas,
            String sinalizacao
    ) {}

    private static final int CARENCIA_MINIMA_CONTRIBUICOES = 12;
    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação pericial e administrativa do INSS. Não substitui análise do segurador.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado e perícia antes de protocolar.";

    public BeneficioIncapacidadeResult avaliar(BeneficioIncapacidadeInput input) {
        List<String> verificados = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();

        switch (input.qualidadeSeguradora()) {
            case ATIVO ->
                verificados.add("Qualidade de segurado ativa — conferida.");
            case PERIODO_GRACA ->
                verificados.add(String.format(
                        "Qualidade de segurado em período de graça (%d meses sem contribuição) — verificar prazo máximo (Lei 8.213/91 art. 15).",
                        input.mesesDesdeUltimaContribuicao()));
            case PERDIDA ->
                pendencias.add(String.format(
                        "Pendência identificada: qualidade de segurado possivelmente perdida — %d meses sem contribuição (Lei 8.213/91 art. 15) — sujeito à verificação do INSS.",
                        input.mesesDesdeUltimaContribuicao()));
        }

        boolean isencaoCarencia = input.acidenteDeTrabalho()
                || input.doencaProfissionalOuDoTrabalho()
                || input.catastrofeOuCrime()
                || input.internacaoHospitalar();

        if (isencaoCarencia) {
            verificados.add("Possível isenção de carência: acidente, doença profissional, catástrofe ou internação — conferir (Lei 8.213/91 art. 26 II).");
        } else if (input.contribuicoesTotais() >= CARENCIA_MINIMA_CONTRIBUICOES) {
            verificados.add(String.format(
                    "Carência possivelmente cumprida: %d contribuições (mínimo: %d) — Lei 8.213/91 art. 25 I.",
                    input.contribuicoesTotais(), CARENCIA_MINIMA_CONTRIBUICOES));
        } else {
            pendencias.add(String.format(
                    "Pendência identificada: carência insuficiente — %d contribuições de %d exigidas (Lei 8.213/91 art. 25 I).",
                    input.contribuicoesTotais(), CARENCIA_MINIMA_CONTRIBUICOES));
        }

        if (!input.laudoPericialApresentado()) {
            pendencias.add("Possível requisito a conferir: laudo médico-pericial não apresentado — necessário para aferição da incapacidade (Lei 8.213/91 art. 43).");
        } else {
            verificados.add("Laudo médico-pericial apresentado — conferido.");
        }

        TipoIncapacidadeIndicada tipo;
        if (input.laudoPericialApresentado() && input.incapacidadePermanenteIndicadaEmLaudo()) {
            tipo = TipoIncapacidadeIndicada.PERMANENTE;
            verificados.add("Laudo indica incapacidade permanente — possível enquadramento em aposentadoria por incapacidade permanente (Lei 8.213/91 art. 42) — sujeito à análise do INSS.");
        } else if (input.laudoPericialApresentado()) {
            tipo = TipoIncapacidadeIndicada.TEMPORARIA;
            verificados.add("Laudo não indica permanência — possível enquadramento em auxílio por incapacidade temporária (Lei 8.213/91 art. 59) — sujeito à análise do INSS.");
        } else {
            tipo = TipoIncapacidadeIndicada.NAO_DETERMINADA;
        }

        if (input.dataInicioIncapacidade() != null && input.dataUltimaContribuicao() != null
                && input.dataInicioIncapacidade().isBefore(input.dataUltimaContribuicao())) {
            pendencias.add("Possível requisito a conferir: data de início da incapacidade anterior à última contribuição — conferir DID/DIB e competência do benefício.");
        }

        return new BeneficioIncapacidadeResult(tipo,
                List.copyOf(verificados), List.copyOf(pendencias),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }
}
