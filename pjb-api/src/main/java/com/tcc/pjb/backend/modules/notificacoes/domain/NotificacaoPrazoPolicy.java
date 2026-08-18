package com.tcc.pjb.backend.modules.notificacoes.domain;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public final class NotificacaoPrazoPolicy {

    private static final Pattern ORIGEM = Pattern.compile("[A-Z0-9._:-]{2,80}");
    private static final int MAX_TITULO = 120;
    private static final int MAX_CORPO = 600;
    private static final LocalTime HORA_PADRAO = LocalTime.of(9, 0);

    public NotificacaoPrazoNormalizada normalizar(Long usuarioId,
                                                  Long processoId,
                                                  String processoNumero,
                                                  LocalDate vencimentoForense,
                                                  LocalDateTime notificarEm,
                                                  String titulo,
                                                  String corpo,
                                                  String urlDetalhes,
                                                  String prioridade,
                                                  String origemModulo,
                                                  String notificationKey) {
        Long usuario = idPositivo(usuarioId, "Usuario destinatario obrigatorio.");
        Long processo = idPositivo(processoId, "Processo obrigatorio para alerta de prazo.");
        if (vencimentoForense == null) {
            throw new NotificacaoPrazoDomainException("Vencimento forense obrigatorio.");
        }
        String origem = normalizarOrigem(origemModulo);
        String tituloSeguro = textoObrigatorio(titulo, "Titulo da notificacao obrigatorio.", MAX_TITULO);
        String corpoSeguro = textoObrigatorio(corpo, "Corpo da notificacao obrigatorio.", MAX_CORPO);
        NotificacaoPrazoPrioridade prioridadeNormalizada = normalizarPrioridade(prioridade);
        String key = notificationKey == null || notificationKey.isBlank()
                ? gerarChave(usuario, processo, vencimentoForense, origem)
                : textoObrigatorio(notificationKey, "Chave de notificacao invalida.", 180);
        LocalDateTime dataNotificacao = normalizarNotificarEm(notificarEm, vencimentoForense, prioridadeNormalizada);
        return new NotificacaoPrazoNormalizada(
                usuario,
                processo,
                textoOpcional(processoNumero, 80),
                vencimentoForense,
                dataNotificacao,
                tituloSeguro,
                corpoSeguro,
                textoOpcional(urlDetalhes, 240),
                prioridadeNormalizada,
                origem,
                key
        );
    }

    public NotificacaoPrazoPrioridade prioridadeParaPrazo(LocalDate vencimentoForense,
                                                          boolean conferenciaManualRecomendada,
                                                          boolean marcoInicialDiaUtil) {
        if (vencimentoForense == null) {
            return NotificacaoPrazoPrioridade.ALTA;
        }
        if (conferenciaManualRecomendada || !marcoInicialDiaUtil) {
            return NotificacaoPrazoPrioridade.CRITICA;
        }
        LocalDate hoje = LocalDate.now();
        if (!vencimentoForense.isAfter(hoje.plusDays(2))) {
            return NotificacaoPrazoPrioridade.ALTA;
        }
        return NotificacaoPrazoPrioridade.NORMAL;
    }

    public String tituloPadrao(String tipoPrazo) {
        String tipo = textoOpcional(tipoPrazo, 80);
        return tipo == null ? "Prazo processual calculado" : "Prazo processual calculado: " + tipo;
    }

    public String corpoPadrao(LocalDate vencimentoForense, boolean conferenciaManualRecomendada) {
        if (vencimentoForense == null) {
            return "Prazo processual calculado sem vencimento forense disponivel.";
        }
        String base = "Vencimento forense: " + vencimentoForense;
        return conferenciaManualRecomendada ? base + ". Conferencia manual recomendada." : base + ".";
    }

    private Long idPositivo(Long value, String message) {
        if (value == null || value < 1) {
            throw new NotificacaoPrazoDomainException(message);
        }
        return value;
    }

    private String normalizarOrigem(String value) {
        if (value == null || value.isBlank()) {
            return "PRAZOS";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!ORIGEM.matcher(normalized).matches()) {
            throw new NotificacaoPrazoDomainException("Origem da notificacao invalida.");
        }
        return normalized;
    }

    private NotificacaoPrazoPrioridade normalizarPrioridade(String value) {
        if (value == null || value.isBlank()) {
            return NotificacaoPrazoPrioridade.NORMAL;
        }
        try {
            return NotificacaoPrazoPrioridade.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new NotificacaoPrazoDomainException("Prioridade de notificacao invalida.");
        }
    }

    private String textoObrigatorio(String value, String message, int max) {
        String normalized = textoOpcional(value, max);
        if (normalized == null) {
            throw new NotificacaoPrazoDomainException(message);
        }
        return normalized;
    }

    private String textoOpcional(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > max) {
            throw new NotificacaoPrazoDomainException("Texto da notificacao excede limite operacional.");
        }
        return normalized;
    }

    private LocalDateTime calcularNotificarEm(LocalDate vencimentoForense, NotificacaoPrazoPrioridade prioridade) {
        int antecedencia = switch (prioridade) {
            case CRITICA -> 0;
            case ALTA -> 1;
            case NORMAL -> 3;
        };
        LocalDate data = vencimentoForense.minusDays(antecedencia);
        LocalDate hoje = LocalDate.now();
        if (data.isBefore(hoje)) {
            data = hoje;
        }
        return LocalDateTime.of(data, HORA_PADRAO);
    }

    private LocalDateTime normalizarNotificarEm(LocalDateTime notificarEm,
                                                LocalDate vencimentoForense,
                                                NotificacaoPrazoPrioridade prioridade) {
        if (notificarEm == null) {
            return calcularNotificarEm(vencimentoForense, prioridade);
        }
        LocalDate hoje = LocalDate.now();
        if (notificarEm.toLocalDate().isBefore(hoje)) {
            throw new NotificacaoPrazoDomainException("Data de notificacao nao pode estar no passado operacional.");
        }
        if (notificarEm.toLocalDate().isAfter(vencimentoForense.plusDays(1))) {
            throw new NotificacaoPrazoDomainException("Data de notificacao posterior ao prazo operacional.");
        }
        return notificarEm;
    }

    private String gerarChave(Long usuarioId, Long processoId, LocalDate vencimento, String origem) {
        String seed = origem + ":" + usuarioId + ":" + processoId + ":" + vencimento;
        return "PRAZO:" + UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
