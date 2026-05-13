package com.tcc.pjb.backend.service.processual.pauta;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarSystemEvent;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarSystemEventRepository;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;

@Service
public class PautaAudienciaNacionalService {

    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.of("-03:00");

    private final UserCalendarSystemEventRepository systemEventRepository;
    private final CalendarioForenseTribunalService calendarioForenseTribunalService;
    private final TribunalRuleEngine tribunalRuleEngine;

    public PautaAudienciaNacionalService(UserCalendarSystemEventRepository systemEventRepository,
                                         CalendarioForenseTribunalService calendarioForenseTribunalService,
                                         TribunalRuleEngine tribunalRuleEngine) {
        this.systemEventRepository = Objects.requireNonNull(systemEventRepository);
        this.calendarioForenseTribunalService = Objects.requireNonNull(calendarioForenseTribunalService);
        this.tribunalRuleEngine = Objects.requireNonNull(tribunalRuleEngine);
    }

    public PautaAudienciaDecision avaliar(PautaAudienciaCommand command) {
        Objects.requireNonNull(command);
        if (command.usuarioId() == null) {
            throw new IllegalArgumentException("Usuário responsável obrigatório para pauta.");
        }
        if (command.inicio() == null) {
            throw new IllegalArgumentException("Data e hora inicial obrigatórias.");
        }
        int duracaoMinutos = command.duracaoMinutos() == null ? 60 : command.duracaoMinutos();
        if (duracaoMinutos <= 0) {
            throw new IllegalArgumentException("Duração inválida.");
        }
        LocalDate data = command.inicio().toLocalDate();
        var diaForense = calendarioForenseTribunalService.analisarDia(
                new CalendarioForenseTribunalService.ContextoCalendario(
                        command.tribunalCodigo(),
                        command.uf(),
                        command.comarca(),
                        command.ramo(),
                        command.grau(),
                        data
                ),
                data
        );
        List<String> conflitos = detectarConflitos(command.usuarioId(), command.inicio(), duracaoMinutos);
        TribunalRuleEngine.ContextoResolucao contexto = TribunalRuleEngine.ContextoResolucao.agora(
                command.tribunalCodigo(),
                command.comarca(),
                resolveVaraId(command),
                command.ramo(),
                command.grau()
        );
        int prazoConciliacaoDias = tribunalRuleEngine.resolverPrazoDias(
                TribunalRuleEngine.ChaveRegra.AUDIENCIA_CONCIL_PRAZO,
                contexto,
                30
        );
        boolean conciliacaoObrigatoria = tribunalRuleEngine.resolverBooleano(
                TribunalRuleEngine.ChaveRegra.AUDIENCIA_CONCIL_OBRIG,
                contexto,
                !isInstrucaoEstrita(command.tipo())
        );
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("Prazo parametrizado para designação: " + prazoConciliacaoDias + " dias.");
        fundamentos.add("Conciliação obrigatória: " + (conciliacaoObrigatoria ? "SIM" : "NAO"));
        if (!diaForense.diaUtil()) {
            fundamentos.add("Dia não útil forense: " + diaForense.motivo());
        }
        if (!conflitos.isEmpty()) {
            fundamentos.add("Conflito de agenda detectado para o responsável informado.");
        }
        LocalDateTime sugestao = nextAvailableSlot(command, duracaoMinutos);
        boolean disponivel = diaForense.diaUtil() && conflitos.isEmpty();
        return new PautaAudienciaDecision(
                disponivel,
                command.inicio(),
                command.inicio().plusMinutes(duracaoMinutos),
                duracaoMinutos,
                diaForense.diaUtil(),
                diaForense.motivo(),
                conflitos,
                sugestao,
                prazoConciliacaoDias,
                conciliacaoObrigatoria,
                List.copyOf(fundamentos),
                null,
                buildDomainKey(command)
        );
    }

    @Transactional
    public PautaAudienciaDecision registrar(PautaAudienciaCommand command) {
        PautaAudienciaDecision decision = avaliar(command);
        if (!decision.disponivel()) {
            throw new IllegalStateException("Slot de pauta indisponível para a audiência informada.");
        }
        String domainKey = buildDomainKey(command);
        UserCalendarSystemEvent event = systemEventRepository.findByUsuarioIdAndDomainKey(command.usuarioId(), domainKey)
                .orElseGet(UserCalendarSystemEvent::new);
        Instant now = Instant.now();
        event.setUsuarioId(command.usuarioId());
        event.setProcessoId(command.processoId());
        event.setDomainKey(domainKey);
        event.setEventType("AUDIENCIA_PROCESSUAL");
        event.setTitle(resolveTitle(command));
        event.setBody(resolveBody(command));
        event.setAt(command.inicio());
        event.setColor("primary");
        event.setDetailsUrl(command.detailsUrl());
        if (event.getCreatedAt() == null) {
            event.setCreatedAt(now);
        }
        event.setUpdatedAt(now);
        systemEventRepository.save(event);
        return new PautaAudienciaDecision(
                true,
                decision.inicio(),
                decision.fim(),
                decision.duracaoMinutos(),
                decision.diaUtilForense(),
                decision.motivoIndisponibilidade(),
                decision.conflitos(),
                decision.sugestaoAlternativa(),
                decision.prazoMaximoDesignacaoDias(),
                decision.conciliacaoObrigatoria(),
                decision.fundamentos(),
                event.getId(),
                domainKey
        );
    }

    public PautaAudienciaDecision registrar(Processo processo,
                                            Usuario usuario,
                                            LocalDateTime inicio,
                                            Integer duracaoMinutos,
                                            String tipo,
                                            String local) {
        Objects.requireNonNull(processo);
        Objects.requireNonNull(usuario);
        return registrar(new PautaAudienciaCommand(
                usuario.getId(),
                processo.getId(),
                Optional.ofNullable(processo.getTribunalCodigoRoteado()).orElse(processo.getTribunal()),
                Optional.ofNullable(processo.getUf()).orElse(usuario.getUf()),
                Optional.ofNullable(processo.getComarca()).orElse(usuario.getComarca()),
                Optional.ofNullable(processo.getRamoDireito()).orElse(RamoDireito.CIVIL),
                GrauJurisdicao.PRIMEIRO_GRAU,
                inicio,
                duracaoMinutos == null ? 60 : duracaoMinutos,
                tipo,
                local,
                "/api/v1/processos/" + processo.getId()
        ));
    }

    private List<String> detectarConflitos(Long usuarioId, LocalDateTime inicio, int duracaoMinutos) {
        LocalDateTime from = inicio.minusMinutes(Math.max(30, duracaoMinutos));
        LocalDateTime to = inicio.plusMinutes(Math.max(30, duracaoMinutos));
        return systemEventRepository.findByUsuarioIdBetween(usuarioId, from, to).stream()
                .map(event -> event.getTitle() + " @ " + event.getAt())
                .toList();
    }

    private LocalDateTime nextAvailableSlot(PautaAudienciaCommand command, int duracaoMinutos) {
        LocalDateTime cursor = command.inicio().plusDays(1);
        for (int i = 0; i < 30; i++) {
            var dia = calendarioForenseTribunalService.analisarDia(
                    new CalendarioForenseTribunalService.ContextoCalendario(
                            command.tribunalCodigo(),
                            command.uf(),
                            command.comarca(),
                            command.ramo(),
                            command.grau(),
                            cursor.toLocalDate()
                    ),
                    cursor.toLocalDate()
            );
            if (dia.diaUtil() && detectarConflitos(command.usuarioId(), cursor, duracaoMinutos).isEmpty()) {
                return cursor;
            }
            cursor = cursor.plusDays(1);
        }
        return command.inicio().plusDays(7);
    }

    private String resolveTitle(PautaAudienciaCommand command) {
        String tipo = command.tipo() == null || command.tipo().isBlank() ? "AUDIENCIA" : command.tipo().trim().toUpperCase();
        return tipo + " - PROCESSO " + Optional.ofNullable(command.processoId()).orElse(0L);
    }

    private String resolveBody(PautaAudienciaCommand command) {
        List<String> parts = new ArrayList<>();
        parts.add("Processo: " + Optional.ofNullable(command.processoId()).orElse(0L));
        parts.add("Local: " + Optional.ofNullable(command.local()).orElse("A DEFINIR"));
        parts.add("Inicio: " + command.inicio());
        parts.add("Duracao: " + Optional.ofNullable(command.duracaoMinutos()).orElse(60) + " min");
        return String.join(" | ", parts);
    }

    private String resolveVaraId(PautaAudienciaCommand command) {
        return "PAUTA_" + Optional.ofNullable(command.comarca()).orElse("GERAL").trim().replace(' ', '_').toUpperCase();
    }

    private String buildDomainKey(PautaAudienciaCommand command) {
        long epoch = command.inicio().toInstant(DEFAULT_OFFSET).toEpochMilli();
        return "AUD:" + command.usuarioId() + ':' + Optional.ofNullable(command.processoId()).orElse(0L) + ':' + epoch;
    }

    private boolean isInstrucaoEstrita(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return false;
        }
        String normalized = tipo.trim().toUpperCase();
        return normalized.contains("INSTRU") || normalized.contains("INTERROG") || normalized.contains("JURI");
    }

    public record PautaAudienciaCommand(
            Long usuarioId,
            Long processoId,
            String tribunalCodigo,
            String uf,
            String comarca,
            RamoDireito ramo,
            GrauJurisdicao grau,
            LocalDateTime inicio,
            Integer duracaoMinutos,
            String tipo,
            String local,
            String detailsUrl) {
    }

    public record PautaAudienciaDecision(
            boolean disponivel,
            LocalDateTime inicio,
            LocalDateTime fim,
            int duracaoMinutos,
            boolean diaUtilForense,
            String motivoIndisponibilidade,
            List<String> conflitos,
            LocalDateTime sugestaoAlternativa,
            int prazoMaximoDesignacaoDias,
            boolean conciliacaoObrigatoria,
            List<String> fundamentos,
            Long eventId,
            String pautaKey) {
    }
}
