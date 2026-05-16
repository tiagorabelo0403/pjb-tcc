package com.tcc.pjb.backend.service.responsabilidadecivil;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResponsabilidadeCivilChecklistService {

    public enum TipoResponsabilidade { SUBJETIVA, OBJETIVA, PRESUMIDA }
    public enum TipoDano { MATERIAL, MORAL, ESTETICO, IMAGEM, LUCROS_CESSANTES }
    public enum OrigemResponsabilidade { FATO_PROPRIO, FATO_TERCEIRO, ATIVIDADE_RISCO, RESPONSABILIDADE_ESTADO, ACIDENTE_CONSUMO }

    public record ResponsabilidadeCivilInput(
            OrigemResponsabilidade origem,
            boolean haProvaAtoIlicito,
            boolean haProbaDano,
            boolean haProvaNexoCausal,
            LocalDate dataFatoOuCiencia,
            boolean culpaConcorrente,
            boolean haProvaExtensaoDano,
            List<TipoDano> tiposDano
    ) {}

    public record RequisitoReparacao(
            String requisito,
            String fundamentoLegal,
            boolean atendido
    ) {}

    public record ResponsabilidadeCivilResult(
            TipoResponsabilidade tipoResponsabilidade,
            boolean prescricaoExpirada,
            long diasParaPrescricao,
            List<RequisitoReparacao> requisitosReparacao,
            List<String> pendenciasIdentificadas,
            List<String> orientacoesJuridicas,
            String sinalizacao
    ) {}

    private static final int ANOS_PRESCRICAO_REPARACAO_CIVIL = 3;
    private static final int ANOS_PRESCRICAO_ESTADO = 5;
    private static final int ANOS_PRESCRICAO_CONSUMO = 5;
    private static final int DIAS_AVISO_PRESCRICAO = 180;

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist orientativo. Não substitui análise de advogado especialista em responsabilidade civil.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista antes de qualquer ato processual.";
    private static final String SINAL_PRESCRITO =
            "Prazo prescricional possivelmente expirado — verificar causas suspensivas e interruptivas (CC arts. 197-204) antes de desistir.";

    public ResponsabilidadeCivilResult avaliar(ResponsabilidadeCivilInput input) {
        List<RequisitoReparacao> requisitos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> orientacoes = new ArrayList<>();

        TipoResponsabilidade tipo = determinarTipo(input);
        verificarRequisitos(input, tipo, requisitos, pendencias);
        verificarPrescricao(input, pendencias, orientacoes);
        verificarTiposDano(input, orientacoes);
        verificarCulpaConcorrente(input, orientacoes);

        boolean prescrito = isPrescrito(input);
        long dias = Math.max(0, calcularDiasParaPrescricao(input));

        String sinal = prescrito ? SINAL_PRESCRITO
                     : pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS
                     : SINAL_COM_PENDENCIAS;

        return new ResponsabilidadeCivilResult(
                tipo,
                prescrito,
                dias,
                List.copyOf(requisitos),
                List.copyOf(pendencias),
                List.copyOf(orientacoes),
                sinal);
    }

    private TipoResponsabilidade determinarTipo(ResponsabilidadeCivilInput input) {
        return switch (input.origem()) {
            case RESPONSABILIDADE_ESTADO, ATIVIDADE_RISCO -> TipoResponsabilidade.OBJETIVA;
            case ACIDENTE_CONSUMO -> TipoResponsabilidade.PRESUMIDA;
            default -> TipoResponsabilidade.SUBJETIVA;
        };
    }

    private void verificarRequisitos(ResponsabilidadeCivilInput input, TipoResponsabilidade tipo,
            List<RequisitoReparacao> requisitos, List<String> pendencias) {

        String fundamentoFato = switch (tipo) {
            case OBJETIVA -> input.origem() == OrigemResponsabilidade.RESPONSABILIDADE_ESTADO
                    ? "CF art. 37 §6º — responsabilidade objetiva do Estado"
                    : "CC art. 927 parágrafo único — atividade de risco";
            case PRESUMIDA -> "CDC art. 12 / CDC art. 14 — fato do produto ou serviço";
            case SUBJETIVA -> "CC art. 186 — ato ilícito com prova de culpa ou dolo";
        };

        requisitos.add(new RequisitoReparacao("Fato gerador do dano", fundamentoFato, input.haProvaAtoIlicito()));
        if (!input.haProvaAtoIlicito()) {
            pendencias.add("Pendência: prova do ato ilícito / fato gerador não confirmada — elemento essencial da responsabilidade civil.");
        }

        requisitos.add(new RequisitoReparacao("Prova do dano sofrido", "CC art. 944", input.haProbaDano()));
        if (!input.haProbaDano()) {
            pendencias.add("Pendência: prova do dano não confirmada — sem dano demonstrado não há obrigação de reparar (CC art. 944).");
        }

        requisitos.add(new RequisitoReparacao("Nexo de causalidade entre fato e dano", "CC art. 403", input.haProvaNexoCausal()));
        if (!input.haProvaNexoCausal()) {
            pendencias.add("Pendência: nexo causal não demonstrado — provar relação direta e imediata entre o fato e o dano (CC art. 403).");
        }

        if (!input.haProvaExtensaoDano()) {
            pendencias.add("Possível requisito: extensão do dano não documentada — necessária para fixação do valor da indenização (CC art. 944). Juntar laudos, orçamentos e recibos.");
        }
    }

    private void verificarPrescricao(ResponsabilidadeCivilInput input,
            List<String> pendencias, List<String> orientacoes) {
        if (input.dataFatoOuCiencia() == null) {
            pendencias.add("Pendência: data do fato ou da ciência do dano não informada — necessária para calcular prescrição.");
            return;
        }

        int anos = switch (input.origem()) {
            case RESPONSABILIDADE_ESTADO -> ANOS_PRESCRICAO_ESTADO;
            case ACIDENTE_CONSUMO -> ANOS_PRESCRICAO_CONSUMO;
            default -> ANOS_PRESCRICAO_REPARACAO_CIVIL;
        };

        String fundamento = switch (input.origem()) {
            case RESPONSABILIDADE_ESTADO -> "DL 4.597/42 art. 1º — prescrição quinquenal";
            case ACIDENTE_CONSUMO -> "CDC art. 27 — prescrição quinquenal";
            default -> "CC art. 206 §3º III — prescrição trienal";
        };

        LocalDate vencimento = input.dataFatoOuCiencia().plusYears(anos);
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), vencimento);

        if (diasRestantes < 0) {
            pendencias.add(String.format(
                    "Prazo prescricional de %d ano(s) possivelmente expirado (%s). Verificar causas" +
                    " suspensivas (CC arts. 197-201) e interruptivas (CC arts. 202-204) antes de desistir.",
                    anos, fundamento));
        } else if (diasRestantes < DIAS_AVISO_PRESCRICAO) {
            pendencias.add(String.format(
                    "Atenção: %d dias para a prescrição (%s). Providenciar distribuição ou ato interruptivo com urgência.",
                    diasRestantes, fundamento));
        } else {
            orientacoes.add(String.format(
                    "Prazo prescricional: %d dia(s) restantes — vencimento em %s (%s).",
                    diasRestantes, vencimento, fundamento));
        }
    }

    private void verificarTiposDano(ResponsabilidadeCivilInput input, List<String> orientacoes) {
        if (input.tiposDano() == null || input.tiposDano().isEmpty()) return;

        if (input.tiposDano().contains(TipoDano.MORAL) && input.tiposDano().contains(TipoDano.MATERIAL)) {
            orientacoes.add("Cumulação de danos moral e material é admitida — STJ Súmula 37.");
        }
        if (input.tiposDano().contains(TipoDano.ESTETICO)) {
            orientacoes.add("Dano estético é autônomo e cumulável com dano moral — STJ Súmula 387.");
        }
        if (input.tiposDano().contains(TipoDano.LUCROS_CESSANTES)) {
            orientacoes.add("Lucros cessantes: demonstrar o que razoavelmente deixou de lucrar (CC art. 402). Exige prova concreta da projeção de ganhos — não mera expectativa.");
        }
        if (input.tiposDano().contains(TipoDano.IMAGEM)) {
            orientacoes.add("Dano à imagem: cumulável com dano moral — verificar incidência de CF art. 5º V e X. Guardar provas de veiculação.");
        }
    }

    private void verificarCulpaConcorrente(ResponsabilidadeCivilInput input, List<String> orientacoes) {
        if (input.culpaConcorrente()) {
            orientacoes.add("Culpa concorrente identificada — o juízo pode reduzir proporcionalmente a indenização (CC art. 945). Documentar o percentual de contribuição de cada parte.");
        }
    }

    private boolean isPrescrito(ResponsabilidadeCivilInput input) {
        if (input.dataFatoOuCiencia() == null) return false;
        int anos = switch (input.origem()) {
            case RESPONSABILIDADE_ESTADO -> ANOS_PRESCRICAO_ESTADO;
            case ACIDENTE_CONSUMO -> ANOS_PRESCRICAO_CONSUMO;
            default -> ANOS_PRESCRICAO_REPARACAO_CIVIL;
        };
        return input.dataFatoOuCiencia().plusYears(anos).isBefore(LocalDate.now());
    }

    private long calcularDiasParaPrescricao(ResponsabilidadeCivilInput input) {
        if (input.dataFatoOuCiencia() == null) return 0;
        int anos = switch (input.origem()) {
            case RESPONSABILIDADE_ESTADO -> ANOS_PRESCRICAO_ESTADO;
            case ACIDENTE_CONSUMO -> ANOS_PRESCRICAO_CONSUMO;
            default -> ANOS_PRESCRICAO_REPARACAO_CIVIL;
        };
        return ChronoUnit.DAYS.between(LocalDate.now(), input.dataFatoOuCiencia().plusYears(anos));
    }
}
