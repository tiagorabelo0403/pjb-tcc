package com.tcc.pjb.backend.service.procuradoria.calendar;

import com.tcc.pjb.backend.service.procuradoria.queue.PrecatorioRpvQueuePlanner;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PrecatorioRpvCalendarPlanner {

    public AgendaPlan plan(AgendaCommand command) {
        Objects.requireNonNull(command, "command");
        List<AgendaLane> lanes = new ArrayList<>();
        List<AgendaEvent> prazos = buildPrazoEvents(command);
        if (!prazos.isEmpty()) {
            lanes.add(new AgendaLane("PRAZO_FINANCEIRO", "Prazo financeiro", List.copyOf(prazos)));
        }
        List<AgendaEvent> fila = buildFilaEvents(command);
        if (!fila.isEmpty()) {
            lanes.add(new AgendaLane("FILA_OPERACIONAL", "Fila operacional", List.copyOf(fila)));
        }
        List<AgendaEvent> governanca = buildGovernancaEvents(command);
        if (!governanca.isEmpty()) {
            lanes.add(new AgendaLane("GOVERNANCA", "Governança e controle", List.copyOf(governanca)));
        }
        return new AgendaPlan(List.copyOf(lanes));
    }

    private List<AgendaEvent> buildPrazoEvents(AgendaCommand command) {
        List<AgendaEvent> events = new ArrayList<>();
        events.add(new AgendaEvent(
                "CALCULO_BASE",
                "Conta-base consolidada",
                command.calculadoEm(),
                "CONCLUIDO",
                "BLUE",
                "Marco interno da apuração financeira que alimenta a requisição de pagamento."
        ));
        if ("RPV".equals(command.modalidade())) {
            events.add(new AgendaEvent(
                    "PRAZO_MAXIMO_RPV",
                    "Controle do prazo máximo da RPV",
                    command.prazoMaximoPagamento(),
                    "CRITICO",
                    "AMBER",
                    "Janela de controle operacional do pagamento de pequeno valor."
            ));
            return events;
        }
        if (command.dataApresentacao() != null) {
            events.add(new AgendaEvent(
                    "APRESENTACAO_TRIBUNAL",
                    "Apresentação do requisitório ao tribunal",
                    command.dataApresentacao(),
                    "EM_FILA",
                    "PURPLE",
                    "Marco usado para ingresso na ordem cronológica da entidade devedora."
            ));
        }
        if (command.pagamentoEstimadoAte() != null) {
            events.add(new AgendaEvent(
                    "PREVISAO_OPERACIONAL_PAGAMENTO",
                    "Previsão operacional de pagamento",
                    command.pagamentoEstimadoAte(),
                    "PREVISTO",
                    "GREEN",
                    "Estimativa operacional do ciclo de pagamento calculada a partir do regime e da fila principal."
            ));
        }
        return events;
    }

    private List<AgendaEvent> buildFilaEvents(AgendaCommand command) {
        List<AgendaEvent> events = new ArrayList<>();
        LocalDate baseDate = command.dataApresentacao() == null ? command.calculadoEm() : command.dataApresentacao();
        if ("RPV".equals(command.modalidade())) {
            events.add(new AgendaEvent(
                    "EXPEDICAO_RPV",
                    "Expedição da RPV",
                    baseDate,
                    "EM_PROCESSAMENTO",
                    "BLUE",
                    "Entrada da requisição na fila de execução direta e conferência formal."
            ));
            events.add(new AgendaEvent(
                    "CONFERENCIA_DEPOSITO_RPV",
                    "Conferência do depósito",
                    command.prazoMaximoPagamento(),
                    "MONITORAMENTO",
                    "AMBER",
                    "Acompanhamento da obrigação financeira até a confirmação do aporte."
            ));
            events.add(new AgendaEvent(
                    "ALVARA_RPV",
                    "Liberação por alvará",
                    command.prazoMaximoPagamento(),
                    "PENDENTE_LIBERACAO",
                    "GREEN",
                    "Último estágio operacional da fila de pequeno valor."
            ));
            return events;
        }
        events.add(new AgendaEvent(
                "CLASSIFICACAO_LISTA",
                "Classificação na ordem cronológica",
                baseDate,
                "EM_FILA",
                "PURPLE",
                "O requisitório ingressa na lista principal da entidade devedora conforme prioridade e natureza."
        ));
        if (command.superpreferencia()) {
            events.add(new AgendaEvent(
                    "SUPERPREFERENCIA",
                    "Superpreferência alimentar",
                    baseDate,
                    "PRIORIDADE_MAXIMA",
                    "RED",
                    "Tratamento preferencial para crédito alimentar com superpreferência reconhecida."
            ));
        }
        if (command.regimeEspecial()) {
            events.add(new AgendaEvent(
                    "REGIME_ESPECIAL",
                    "Regime especial ativo",
                    baseDate,
                    "GOVERNADO",
                    "AMBER",
                    "Fila sujeita ao plano anual de redução de estoque e controle cronológico especial."
            ));
        }
        if (command.acordoDiretoHabilitado() && command.regimeEspecial()) {
            events.add(new AgendaEvent(
                    "ACORDO_DIRETO",
                    "Janela de acordo direto",
                    baseDate,
                    "FACULTATIVO",
                    "BLUE",
                    "Trilha facultativa de acordo direto quando houver regulamentação local válida."
            ));
        }
        if (command.pagamentoEstimadoAte() != null) {
            events.add(new AgendaEvent(
                    "ALVARA_PRECATORIO",
                    "Liberação estimada por alvará",
                    command.pagamentoEstimadoAte(),
                    "PREVISTO",
                    "GREEN",
                    "Etapa final após aporte, conferência e liberação judicial."
            ));
        }
        return events;
    }

    private List<AgendaEvent> buildGovernancaEvents(AgendaCommand command) {
        List<AgendaEvent> events = new ArrayList<>();
        LocalDate reference = command.dataApresentacao() == null ? command.calculadoEm() : command.dataApresentacao();
        PrecatorioRpvQueuePlanner.QueueGovernance governanca = command.governanca();
        if (governanca == null) {
            return events;
        }
        if (governanca.publicarListaCronologicaAnonimizada()) {
            events.add(new AgendaEvent(
                    "PUBLICACAO_LISTA",
                    "Publicação da lista cronológica",
                    reference,
                    "TRANSPARENCIA",
                    "BLUE",
                    "A governança prevê divulgação da lista de ordem cronológica de forma anonimizada."
            ));
        }
        if (governanca.monitorarSequestroPorBurlaOuNaoAlocacao()) {
            events.add(new AgendaEvent(
                    "MONITORAMENTO_SEQUESTRO",
                    "Monitoramento de sequestro",
                    command.pagamentoEstimadoAte(),
                    "CONTROLE",
                    "RED",
                    "A fila mantém vigilância para hipóteses de burla ou não alocação financeira."
            ));
        }
        return events;
    }

    public record AgendaCommand(
            String modalidade,
            LocalDate calculadoEm,
            LocalDate dataApresentacao,
            LocalDate prazoMaximoPagamento,
            LocalDate pagamentoEstimadoAte,
            boolean superpreferencia,
            boolean regimeEspecial,
            boolean acordoDiretoHabilitado,
            PrecatorioRpvQueuePlanner.QueueGovernance governanca
    ) {
    }

    public record AgendaPlan(
            List<AgendaLane> lanes
    ) {
    }

    public record AgendaLane(
            String laneCode,
            String laneTitle,
            List<AgendaEvent> events
    ) {
    }

    public record AgendaEvent(
            String eventCode,
            String title,
            LocalDate scheduledDate,
            String status,
            String color,
            String details
    ) {
    }
}
