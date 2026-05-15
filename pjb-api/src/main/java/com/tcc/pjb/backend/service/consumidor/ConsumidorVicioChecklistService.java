package com.tcc.pjb.backend.service.consumidor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConsumidorVicioChecklistService {

    public enum TipoBem { DURAVEL, NAO_DURAVEL }
    public enum TipoVicio { PRODUTO, SERVICO, QUANTIDADE }
    public enum CanalAquisicao { ESTABELECIMENTO_FISICO, FORA_ESTABELECIMENTO }

    public record ConsumidorVicioInput(
            String cpf,
            LocalDate dataAquisicao,
            LocalDate dataConstatacaoVicio,
            TipoBem tipoBem,
            TipoVicio tipoVicio,
            BigDecimal valorProduto,
            CanalAquisicao canalAquisicao,
            boolean vicioOculto
    ) {}

    public record DireitoIndicado(
            String descricao,
            String fundamentoLegal,
            String observacao
    ) {}

    public record ConsumidorVicioResult(
            int diasDecadencialTotal,
            int diasDecadencialRestantes,
            boolean direitoArrependimentoEligivel,
            List<DireitoIndicado> direitosIndicados,
            List<String> pendenciasIdentificadas,
            List<String> requisitosVerificados,
            String sinalizacao
    ) {}

    private static final int PRAZO_DECADENCIAL_DURAVEL_DIAS = 90;
    private static final int PRAZO_DECADENCIAL_NAO_DURAVEL_DIAS = 30;
    private static final int PRAZO_ARREPENDIMENTO_DIAS = 7;
    private static final int ANOS_PRESCRICAO_DANOS = 5;
    private static final int DIAS_SANEAMENTO_VICIO = 30;

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação jurídica. Não substitui análise do advogado especialista em direito do consumidor.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista em direito do consumidor antes de qualquer reclamação formal ou judicial.";

    public ConsumidorVicioResult avaliar(ConsumidorVicioInput input) {
        List<DireitoIndicado> direitos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> verificados = new ArrayList<>();

        int prazoDecadencial = input.tipoBem() == TipoBem.DURAVEL
                ? PRAZO_DECADENCIAL_DURAVEL_DIAS
                : PRAZO_DECADENCIAL_NAO_DURAVEL_DIAS;

        int diasRestantes = calcularDecadencia(input, prazoDecadencial, pendencias, verificados);
        boolean arrependimentoEligivel = verificarArrependimento(input, direitos, pendencias, verificados);

        mapearDireitosPorTipo(input, direitos, verificados);
        verificarPrescricaoDanos(input, pendencias, verificados);

        return new ConsumidorVicioResult(
                prazoDecadencial,
                diasRestantes,
                arrependimentoEligivel,
                List.copyOf(direitos),
                List.copyOf(pendencias),
                List.copyOf(verificados),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private int calcularDecadencia(ConsumidorVicioInput input, int prazoTotal,
            List<String> pendencias, List<String> verificados) {
        LocalDate dataInicio = input.vicioOculto() ? input.dataConstatacaoVicio() : input.dataAquisicao();

        if (dataInicio == null) {
            pendencias.add("Pendência identificada: data de " +
                    (input.vicioOculto() ? "constatação do vício oculto" : "aquisição") +
                    " não informada — necessária para apuração do prazo decadencial (CDC art. 26).");
            return prazoTotal;
        }

        long diasDecorridos = ChronoUnit.DAYS.between(dataInicio, LocalDate.now());
        int restantes = (int) Math.max(0, prazoTotal - diasDecorridos);
        String inciso = input.tipoBem() == TipoBem.DURAVEL ? "II" : "I";

        if (restantes == 0) {
            pendencias.add(String.format(
                    "Pendência identificada: prazo decadencial possivelmente expirado — %d dias decorridos desde %s" +
                    " (prazo: %d dias, CDC art. 26 %s). Conferir com urgência se há causa suspensiva ou interruptiva.",
                    diasDecorridos, dataInicio, prazoTotal, inciso));
        } else if (restantes <= prazoTotal * 0.2) {
            pendencias.add(String.format(
                    "Possível requisito a conferir: apenas %d dias restantes no prazo decadencial de %d dias" +
                    " (CDC art. 26 %s) — conferir urgência para reclamação formal.",
                    restantes, prazoTotal, inciso));
        } else {
            verificados.add(String.format(
                    "Prazo decadencial: %d dias restantes de %d (CDC art. 26 %s) — dentro do prazo.",
                    restantes, prazoTotal, inciso));
        }

        if (input.vicioOculto()) {
            verificados.add("Vício oculto: prazo decadencial conta a partir da manifestação (CDC art. 26 §3º)" +
                    " — sujeito à comprovação do momento da descoberta.");
        }

        return restantes;
    }

    private boolean verificarArrependimento(ConsumidorVicioInput input,
            List<DireitoIndicado> direitos, List<String> pendencias, List<String> verificados) {
        if (input.canalAquisicao() != CanalAquisicao.FORA_ESTABELECIMENTO) {
            verificados.add("Compra em estabelecimento físico — direito de arrependimento (CDC art. 49) não aplicável por padrão.");
            return false;
        }

        if (input.dataAquisicao() == null) {
            pendencias.add("Pendência identificada: data de aquisição não informada — necessária para verificar prazo de arrependimento (CDC art. 49).");
            return true;
        }

        long diasDesdeCompra = ChronoUnit.DAYS.between(input.dataAquisicao(), LocalDate.now());

        if (diasDesdeCompra <= PRAZO_ARREPENDIMENTO_DIAS) {
            direitos.add(new DireitoIndicado(
                    "Direito de arrependimento",
                    "CDC art. 49",
                    String.format("Compra fora do estabelecimento — possível exercer arrependimento em até 7 dias da aquisição" +
                            " ou recebimento (%d dias decorridos). Conferir se ainda está no prazo.", diasDesdeCompra)));
            verificados.add(String.format(
                    "Direito de arrependimento disponível — %d dias decorridos de 7 (CDC art. 49).", diasDesdeCompra));
            return true;
        }

        pendencias.add(String.format(
                "Possível requisito a conferir: prazo de arrependimento de 7 dias pode ter expirado" +
                " (%d dias desde a compra, CDC art. 49) — verificar data de recebimento do produto.",
                diasDesdeCompra));
        return false;
    }

    private void mapearDireitosPorTipo(ConsumidorVicioInput input,
            List<DireitoIndicado> direitos, List<String> verificados) {
        switch (input.tipoVicio()) {
            case PRODUTO -> mapearVicioProduto(input, direitos, verificados);
            case SERVICO -> mapearVicioServico(input, direitos, verificados);
            case QUANTIDADE -> mapearVicioQuantidade(input, direitos, verificados);
        }
    }

    private void mapearVicioProduto(ConsumidorVicioInput input,
            List<DireitoIndicado> direitos, List<String> verificados) {
        verificados.add(String.format(
                "Vício de produto — fornecedor tem %d dias para sanar (CDC art. 18 §1º)." +
                " Se não sanado: substituição, restituição ou abatimento proporcional.", DIAS_SANEAMENTO_VICIO));

        direitos.add(new DireitoIndicado(
                "Prazo de saneamento pelo fornecedor (30 dias)",
                "CDC art. 18 §1º",
                "Possível requisito a conferir: fornecedor deve sanar o vício em até 30 dias." +
                " Se não sanado, consumidor pode exigir substituição, restituição integral ou abatimento proporcional."));

        direitos.add(new DireitoIndicado(
                "Substituição do produto por outro da mesma espécie",
                "CDC art. 18 §1º I",
                "Direito à substituição por produto equivalente em perfeitas condições" +
                " — sujeito à disponibilidade e ao não saneamento no prazo de 30 dias."));

        String valorStr = input.valorProduto() != null
                ? "R$ " + input.valorProduto().toPlainString()
                : "valor não informado";

        direitos.add(new DireitoIndicado(
                "Restituição do valor pago",
                "CDC art. 18 §1º II",
                String.format("Estimativa: %s — sujeita à confirmação do valor pago e à prova do vício.", valorStr)));

        direitos.add(new DireitoIndicado(
                "Abatimento proporcional do preço",
                "CDC art. 18 §1º III",
                "Redução proporcional ao defeito constatado — exige laudo técnico para quantificação."));

        verificados.add("Responsabilidade solidária de todos os fornecedores da cadeia produtiva (CDC art. 18 caput)" +
                " — possível acionar fabricante, importador ou comerciante.");
    }

    private void mapearVicioServico(ConsumidorVicioInput input,
            List<DireitoIndicado> direitos, List<String> verificados) {
        verificados.add("Vício de serviço — fornecedor responde pela reexecução sem custo adicional ou restituição (CDC art. 20).");

        String valorStr = input.valorProduto() != null
                ? "R$ " + input.valorProduto().toPlainString()
                : "valor não informado";

        direitos.add(new DireitoIndicado(
                "Reexecução do serviço sem custo",
                "CDC art. 20 I",
                "Serviço refeito por outro prestador qualificado às custas do fornecedor original" +
                " — sujeito à comprovação do vício."));

        direitos.add(new DireitoIndicado(
                "Restituição do valor pago pelo serviço",
                "CDC art. 20 II",
                String.format("Estimativa: %s — sujeita à confirmação do valor e à prova do vício.", valorStr)));

        direitos.add(new DireitoIndicado(
                "Abatimento proporcional do preço do serviço",
                "CDC art. 20 III",
                "Redução proporcional ao vício verificado — exige comprovação técnica."));
    }

    private void mapearVicioQuantidade(ConsumidorVicioInput input,
            List<DireitoIndicado> direitos, List<String> verificados) {
        verificados.add("Vício de quantidade — fornecedor responde pela complementação, substituição, abatimento ou restituição (CDC art. 19).");

        String valorStr = input.valorProduto() != null
                ? "R$ " + input.valorProduto().toPlainString()
                : "valor não informado";

        direitos.add(new DireitoIndicado(
                "Complementação de peso ou medida",
                "CDC art. 19 I",
                "Fornecedor deve completar a quantidade faltante sem custo adicional" +
                " — conferir laudo metrológico se necessário."));

        direitos.add(new DireitoIndicado(
                "Substituição do produto por outro da mesma espécie",
                "CDC art. 19 II",
                "Substituição por produto íntegro com quantidade correta."));

        direitos.add(new DireitoIndicado(
                "Abatimento proporcional do preço",
                "CDC art. 19 III",
                "Redução do preço na proporção da quantidade faltante."));

        direitos.add(new DireitoIndicado(
                "Restituição imediata do valor pago",
                "CDC art. 19 IV",
                String.format("Estimativa: %s — sujeita à confirmação do valor.", valorStr)));
    }

    private void verificarPrescricaoDanos(ConsumidorVicioInput input,
            List<String> pendencias, List<String> verificados) {
        if (input.dataAquisicao() == null) return;

        long anosDecorridos = ChronoUnit.YEARS.between(input.dataAquisicao(), LocalDate.now());

        if (anosDecorridos >= ANOS_PRESCRICAO_DANOS) {
            pendencias.add(String.format(
                    "Pendência identificada: prazo prescricional de 5 anos para reparação de danos pode estar expirado" +
                    " (%d ano(s) desde a aquisição — CDC art. 27). Verificar com urgência.", anosDecorridos));
        } else if (anosDecorridos >= 4) {
            pendencias.add(String.format(
                    "Possível requisito a conferir: %d ano(s) desde a aquisição — prazo prescricional de 5 anos para danos" +
                    " se aproxima (CDC art. 27). Conferir urgência para eventual ação de reparação.", anosDecorridos));
        } else {
            verificados.add(String.format(
                    "Prazo prescricional para danos: %d ano(s) decorrido(s) de 5 (CDC art. 27) — dentro do prazo.",
                    anosDecorridos));
        }
    }
}
