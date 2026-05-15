package com.tcc.pjb.backend.service.usucapiao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UsucapiaoChecklistService {

    public enum ModalidadeUsucapiao {
        EXTRAORDINARIA,
        ORDINARIA,
        ESPECIAL_URBANA,
        ESPECIAL_RURAL,
        FAMILIAR
    }

    public record UsucapiaoInput(
            String cpfPosseiro,
            LocalDate dataInicioPosseManso,
            ModalidadeUsucapiao modalidade,
            BigDecimal areaM2,
            boolean posseComAnimusDomini,
            boolean posseIninterrupta,
            boolean posseSemOposicao,
            boolean possuiOutroImovel,
            boolean utilizaComoMoradia,
            boolean utilizaParaProducao,
            boolean tituloDominioJusto,
            boolean boaFe
    ) {}

    public record RequisitoPosseAnalise(
            String descricao,
            String fundamentoLegal,
            String statusConferencia,
            String observacao
    ) {}

    public record UsucapiaoResult(
            boolean prazoLegalAtingido,
            int anosNecessarios,
            long anosPosse,
            List<RequisitoPosseAnalise> requisitosAnalisados,
            List<String> pendenciasIdentificadas,
            List<String> requisitosVerificados,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação jurídica e pericial. Não substitui análise do advogado nem decisão judicial.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista em direito imobiliário antes de qualquer ação de usucapião.";
    private static final String STATUS_ATENDIDO = "Possível requisito atendido — sujeito à conferência judicial";
    private static final String STATUS_PENDENTE = "Pendência identificada — verificar";
    private static final String STATUS_NAO_VERIFICAVEL = "Não verificável neste checklist — documentar com prova testemunhal e pericial";

    public UsucapiaoResult avaliar(UsucapiaoInput input) {
        List<RequisitoPosseAnalise> requisitos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> verificados = new ArrayList<>();

        int anosNecessarios = resolverAnosNecessarios(input.modalidade());
        long anosPosse = calcularAnosPosse(input, pendencias, verificados);
        boolean prazoAtingido = anosPosse >= anosNecessarios;

        verificarPrazo(input, anosPosse, anosNecessarios, prazoAtingido, requisitos, pendencias, verificados);
        verificarRequisitosEspecificos(input, requisitos, pendencias, verificados);
        verificarArea(input, requisitos, pendencias, verificados);
        adicionarOrientacoesGerais(requisitos, verificados);

        return new UsucapiaoResult(
                prazoAtingido,
                anosNecessarios,
                anosPosse,
                List.copyOf(requisitos),
                List.copyOf(pendencias),
                List.copyOf(verificados),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private int resolverAnosNecessarios(ModalidadeUsucapiao modalidade) {
        return switch (modalidade) {
            case EXTRAORDINARIA -> 15;
            case ORDINARIA -> 10;
            case ESPECIAL_URBANA -> 5;
            case ESPECIAL_RURAL -> 5;
            case FAMILIAR -> 2;
        };
    }

    private long calcularAnosPosse(UsucapiaoInput input,
            List<String> pendencias, List<String> verificados) {
        if (input.dataInicioPosseManso() == null) {
            pendencias.add("Pendência identificada: data de início da posse mansa e pacífica não informada — essencial para apurar o prazo de usucapião (CC art. 1.238 e ss.).");
            return 0;
        }
        long anos = ChronoUnit.YEARS.between(input.dataInicioPosseManso(), LocalDate.now());
        verificados.add(String.format("Posse informada desde %s — %d ano(s) decorridos.", input.dataInicioPosseManso(), anos));
        return anos;
    }

    private void verificarPrazo(UsucapiaoInput input, long anosPosse, int anosNecessarios,
            boolean prazoAtingido, List<RequisitoPosseAnalise> requisitos,
            List<String> pendencias, List<String> verificados) {
        String fundamento = resolverFundamentoLegal(input.modalidade());

        if (!prazoAtingido) {
            long anosFaltantes = anosNecessarios - anosPosse;
            pendencias.add(String.format(
                    "Pendência identificada: prazo de posse insuficiente para %s — %d ano(s) de posse informados;" +
                    " necessários %d anos (%s). Faltam aproximadamente %d ano(s).",
                    input.modalidade().name().toLowerCase().replace('_', ' '),
                    anosPosse, anosNecessarios, fundamento, anosFaltantes));
            requisitos.add(new RequisitoPosseAnalise(
                    String.format("Prazo mínimo de posse: %d anos", anosNecessarios),
                    fundamento,
                    STATUS_PENDENTE,
                    String.format("%d ano(s) de posse — faltam %d ano(s) para o prazo mínimo.", anosPosse, anosFaltantes)));
        } else {
            verificados.add(String.format(
                    "Prazo de posse: %d ano(s) — atende ao mínimo de %d anos para usucapião %s (%s).",
                    anosPosse, anosNecessarios,
                    input.modalidade().name().toLowerCase().replace('_', ' '), fundamento));
            requisitos.add(new RequisitoPosseAnalise(
                    String.format("Prazo mínimo de posse: %d anos", anosNecessarios),
                    fundamento,
                    STATUS_ATENDIDO,
                    String.format("%d ano(s) de posse — sujeito à comprovação documental e testemunhal.", anosPosse)));
        }
    }

    private String resolverFundamentoLegal(ModalidadeUsucapiao modalidade) {
        return switch (modalidade) {
            case EXTRAORDINARIA -> "CC art. 1.238";
            case ORDINARIA -> "CC art. 1.242";
            case ESPECIAL_URBANA -> "CC art. 1.240; CF art. 183";
            case ESPECIAL_RURAL -> "CC art. 1.239; CF art. 191";
            case FAMILIAR -> "CC art. 1.240-A";
        };
    }

    private void verificarRequisitosEspecificos(UsucapiaoInput input,
            List<RequisitoPosseAnalise> requisitos, List<String> pendencias, List<String> verificados) {
        requisitos.add(new RequisitoPosseAnalise(
                "Posse com animus domini (intenção de dono)",
                "CC art. 1.238 caput",
                input.posseComAnimusDomini() ? STATUS_ATENDIDO : STATUS_PENDENTE,
                "Posse deve ser exercida como se o possuidor fosse o proprietário — não mero detentor ou permissionário."));

        requisitos.add(new RequisitoPosseAnalise(
                "Posse mansa, pacífica e ininterrupta",
                "CC art. 1.238 caput",
                (input.posseIninterrupta() && input.posseSemOposicao()) ? STATUS_ATENDIDO : STATUS_PENDENTE,
                "Não pode haver interrupção (perda da posse por mais de ano e dia — CC art. 1.243) nem oposição do proprietário registrado."));

        if (!input.posseComAnimusDomini()) {
            pendencias.add("Pendência identificada: animus domini não confirmado — posse derivada de contrato (comodato, locação) não gera usucapião (CC art. 1.197).");
        }
        if (!input.posseIninterrupta()) {
            pendencias.add("Pendência identificada: posse com interrupção informada — verificar se houve perda da posse por mais de ano e dia (CC art. 1.224) e se o prazo reiniciou.");
        }
        if (!input.posseSemOposicao()) {
            pendencias.add("Possível requisito a conferir: oposição à posse registrada — verificar se há ação reivindicatória, turbação ou esbulho em andamento que comprometa a mansidão da posse.");
        }

        switch (input.modalidade()) {
            case ESPECIAL_URBANA -> verificarEspecialUrbana(input, requisitos, pendencias, verificados);
            case ESPECIAL_RURAL -> verificarEspecialRural(input, requisitos, pendencias, verificados);
            case ORDINARIA -> verificarOrdinaria(input, requisitos, pendencias, verificados);
            case FAMILIAR -> verificarFamiliar(input, requisitos, pendencias, verificados);
            case EXTRAORDINARIA -> verificados.add("Usucapião extraordinária: não exige título nem boa-fé (CC art. 1.238 parágrafo único).");
        }
    }

    private void verificarEspecialUrbana(UsucapiaoInput input,
            List<RequisitoPosseAnalise> requisitos, List<String> pendencias, List<String> verificados) {
        if (!input.utilizaComoMoradia()) {
            pendencias.add("Pendência identificada: usucapião especial urbana exige utilização do imóvel para moradia própria ou da família (CF art. 183 caput).");
        }
        if (input.possuiOutroImovel()) {
            pendencias.add("Pendência identificada: usucapião especial urbana é vedada a quem possui outro imóvel urbano ou rural (CF art. 183 §3º; CC art. 1.240 §2º).");
        }
        requisitos.add(new RequisitoPosseAnalise(
                "Não possuir outro imóvel",
                "CC art. 1.240 §2º; CF art. 183 §3º",
                !input.possuiOutroImovel() ? STATUS_ATENDIDO : STATUS_PENDENTE,
                "Requisito impeditivo: possuir outro imóvel (urbano ou rural) afasta o direito à usucapião especial urbana."));
    }

    private void verificarEspecialRural(UsucapiaoInput input,
            List<RequisitoPosseAnalise> requisitos, List<String> pendencias, List<String> verificados) {
        if (!input.utilizaParaProducao()) {
            pendencias.add("Pendência identificada: usucapião especial rural (pro labore) exige que o posseiro torne a terra produtiva com seu trabalho ou de sua família (CF art. 191 caput).");
        }
        if (input.possuiOutroImovel()) {
            pendencias.add("Pendência identificada: usucapião especial rural é vedada a quem possui outro imóvel rural ou urbano (CF art. 191 parágrafo único).");
        }
        requisitos.add(new RequisitoPosseAnalise(
                "Tornar a terra produtiva (pro labore)",
                "CF art. 191; CC art. 1.239",
                input.utilizaParaProducao() ? STATUS_ATENDIDO : STATUS_PENDENTE,
                "Posseiro deve usar a terra para subsistência ou trabalho — não basta posse; exige labor efetivo."));
    }

    private void verificarOrdinaria(UsucapiaoInput input,
            List<RequisitoPosseAnalise> requisitos, List<String> pendencias, List<String> verificados) {
        if (!input.tituloDominioJusto()) {
            pendencias.add("Possível requisito a conferir: usucapião ordinária exige justo título (CC art. 1.242) para o prazo de 10 anos; sem justo título, avaliar usucapião extraordinária (15 anos).");
        }
        if (!input.boaFe()) {
            pendencias.add("Possível requisito a conferir: usucapião ordinária exige boa-fé do posseiro (CC art. 1.242).");
        }
        requisitos.add(new RequisitoPosseAnalise(
                "Justo título e boa-fé",
                "CC art. 1.242",
                (input.tituloDominioJusto() && input.boaFe()) ? STATUS_ATENDIDO : STATUS_PENDENTE,
                "Justo título: documento que seria hábil a transferir domínio se não tivesse vício — ex: escritura com vício de nulidade."));
    }

    private void verificarFamiliar(UsucapiaoInput input,
            List<RequisitoPosseAnalise> requisitos, List<String> pendencias, List<String> verificados) {
        if (!input.utilizaComoMoradia()) {
            pendencias.add("Pendência identificada: usucapião familiar (CC art. 1.240-A) exige que o posseiro utilize o imóvel como moradia própria ou de seus filhos.");
        }
        if (input.possuiOutroImovel()) {
            pendencias.add("Pendência identificada: usucapião familiar exige que o posseiro não seja proprietário de outro imóvel (CC art. 1.240-A).");
        }
        requisitos.add(new RequisitoPosseAnalise(
                "Abandono do lar pelo ex-cônjuge/companheiro e uso exclusivo como moradia",
                "CC art. 1.240-A",
                STATUS_NAO_VERIFICAVEL,
                "Usucapião familiar pressupõe abandono do lar pelo cônjuge ou companheiro — verificar situação fática e documental."));
        pendencias.add("Possível requisito a conferir: usucapião familiar (CC art. 1.240-A) exige prova de abandono do lar pelo ex-cônjuge/companheiro — verificar documentação e situação fática com advogado.");
    }

    private void verificarArea(UsucapiaoInput input,
            List<RequisitoPosseAnalise> requisitos, List<String> pendencias, List<String> verificados) {
        if (input.areaM2() == null) {
            pendencias.add("Pendência identificada: área do imóvel não informada — necessária para verificar limites das modalidades especiais.");
            return;
        }

        switch (input.modalidade()) {
            case ESPECIAL_URBANA -> {
                if (input.areaM2().compareTo(new BigDecimal("250")) > 0) {
                    pendencias.add(String.format(
                            "Pendência identificada: área de %.0f m² excede o limite de 250 m² para usucapião especial urbana (CF art. 183; CC art. 1.240).",
                            input.areaM2().doubleValue()));
                } else {
                    verificados.add(String.format("Área: %.0f m² — dentro do limite de 250 m² para usucapião especial urbana (CC art. 1.240).", input.areaM2().doubleValue()));
                }
            }
            case ESPECIAL_RURAL -> {
                if (input.areaM2().compareTo(new BigDecimal("500000")) > 0) {
                    pendencias.add(String.format(
                            "Pendência identificada: área de %.0f m² excede o limite de 50 hectares para usucapião especial rural (CF art. 191; CC art. 1.239).",
                            input.areaM2().doubleValue()));
                } else {
                    verificados.add(String.format("Área: %.0f m² — dentro do limite de 50 ha para usucapião especial rural (CC art. 1.239).", input.areaM2().doubleValue()));
                }
            }
            case FAMILIAR -> {
                if (input.areaM2().compareTo(new BigDecimal("250")) > 0) {
                    pendencias.add(String.format(
                            "Pendência identificada: área de %.0f m² excede o limite de 250 m² para usucapião familiar (CC art. 1.240-A).",
                            input.areaM2().doubleValue()));
                } else {
                    verificados.add(String.format("Área: %.0f m² — dentro do limite de 250 m² para usucapião familiar (CC art. 1.240-A).", input.areaM2().doubleValue()));
                }
            }
            default -> verificados.add(String.format("Área informada: %.0f m² — sem limite legal por área nesta modalidade.", input.areaM2().doubleValue()));
        }
    }

    private void adicionarOrientacoesGerais(List<RequisitoPosseAnalise> requisitos, List<String> verificados) {
        requisitos.add(new RequisitoPosseAnalise(
                "Registro da sentença no Cartório de Registro de Imóveis",
                "Lei 6.015/73 art. 167 I 28; CC art. 1.241",
                STATUS_NAO_VERIFICAVEL,
                "A sentença de usucapião deve ser registrada para produzir efeitos erga omnes e possibilitar alienação do imóvel."));
        verificados.add("Usucapião: sujeito à prova pericial de área, testemunhal de posse e manifestação do Ministério Público (CPC art. 721) — checklist é sinal auxiliar apenas.");
    }
}
