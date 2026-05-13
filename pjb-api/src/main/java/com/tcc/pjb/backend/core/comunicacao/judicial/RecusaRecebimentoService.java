package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecusaRecebimentoService {

    private static final Logger log = LoggerFactory.getLogger(RecusaRecebimentoService.class);
    private static final String RESOURCE_TYPE = "RECUSA_RECEBIMENTO";
    private static final String DOMAIN_RECUSA = "RECUSA_RECEBIMENTO_REGISTRO";

    public enum TipoEvidenciaRecusa {
        FOTO_DESTINATARIO_PRESENTE,
        BIOMETRIA_OFICIAL,
        ASSINATURA_DIGITAL_OFICIAL,
        TESTEMUNHA_PRESENCIAL,
        VIDEO_CURTO,
        GPS_CONFIRMADO,
        PORTEIRO_TESTEMUNHA,
        VIZINHO_TESTEMUNHA
    }

    public record RegistroRecusa(
            String registroUuid,
            String mandadoId,
            Long processoId,
            Long oficialId,
            String destinatarioNome,
            String destinatarioDocumento,
            Instant recusadaEm,
            double latitude,
            double longitude,
            String enderecoConfirmado,
            List<TipoEvidenciaRecusa> evidencias,
            String fotoHashBase64,
            String biometriaHashOficial,
            String observacoes,
            String certidaoDeRecusa,
            String hashIntegridade,
            PjbHardwareSecurityModule.AssinaturaHsm assinaturaHsm,
            boolean citacaoEfetivadaPorRecusa,
            boolean publicadoNoDje
    ) {
    }

    public record SolicitacaoRecusa(
            String mandadoId,
            Long processoId,
            Long oficialId,
            String destinatarioNome,
            String destinatarioDocumento,
            double latitude,
            double longitude,
            String enderecoConfirmado,
            List<TipoEvidenciaRecusa> evidencias,
            String fotoHashBase64,
            String biometriaHashOficial,
            String observacoes
    ) {
    }

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final AuditLedgerService auditLedger;
    private final NotificacaoInteligentePJB notificacaoEngine;
    private final PjbHardwareSecurityModule hsm;
    private final GeofencePresencaOficialService geofenceService;
    private final ObjectProvider<WebhookOutboundService> webhookProvider;
    private final ObjectProvider<PrazoRespostaPosEntregaEngine> prazoProvider;
    private final ObjectProvider<ReveliaAutomaticaEngine> reveliaProvider;
    private final ComunicacaoJudicialStateStore stateStore;
    private final Cache<String, RegistroRecusa> registrosPorMandado = Caffeine.newBuilder()
            .maximumSize(20000)
            .expireAfterAccess(Duration.ofHours(24))
            .build();

    public RecusaRecebimentoService(ProcessoRepository processoRepository,
                                    UsuarioRepository usuarioRepository,
                                    ExpedicaoJudicialRepository expedicaoRepository,
                                    AuditLedgerService auditLedger,
                                    NotificacaoInteligentePJB notificacaoEngine,
                                    PjbHardwareSecurityModule hsm,
                                    GeofencePresencaOficialService geofenceService,
                                    ObjectProvider<WebhookOutboundService> webhookProvider,
                                    ObjectProvider<PrazoRespostaPosEntregaEngine> prazoProvider,
                                    ObjectProvider<ReveliaAutomaticaEngine> reveliaProvider,
                                    ComunicacaoJudicialStateStore stateStore) {
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.notificacaoEngine = Objects.requireNonNull(notificacaoEngine, "notificacaoEngine");
        this.hsm = Objects.requireNonNull(hsm, "hsm");
        this.geofenceService = Objects.requireNonNull(geofenceService, "geofenceService");
        this.webhookProvider = Objects.requireNonNull(webhookProvider, "webhookProvider");
        this.prazoProvider = Objects.requireNonNull(prazoProvider, "prazoProvider");
        this.reveliaProvider = Objects.requireNonNull(reveliaProvider, "reveliaProvider");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    @Transactional
    public RegistroRecusa registrar(SolicitacaoRecusa solicitacao) {
        Objects.requireNonNull(solicitacao, "solicitacao");
        validar(solicitacao);
        GeofencePresencaOficialService.ValidacaoPresenca geofence = geofenceService.validar(
                solicitacao.mandadoId(),
                solicitacao.oficialId(),
                solicitacao.latitude(),
                solicitacao.longitude()
        );
        if (!geofence.liberadoParaRegistro()) {
            throw new IllegalStateException("Oficial fora do geofence autorizado para o mandado: " + solicitacao.mandadoId());
        }
        String certidao = gerarCertidaoRecusa(solicitacao, geofence.distanciaMetros());
        byte[] certidaoBytes = certidao.getBytes(StandardCharsets.UTF_8);
        PjbHardwareSecurityModule.AssinaturaHsm assinatura = hsm.assinar(certidaoBytes);
        String hashIntegridade = sha256Hex(
                solicitacao.mandadoId()
                        + solicitacao.destinatarioDocumento()
                        + solicitacao.latitude()
                        + solicitacao.longitude()
                        + Instant.now().toEpochMilli()
        );
        RegistroRecusa registro = new RegistroRecusa(
                UUID.randomUUID().toString(),
                solicitacao.mandadoId(),
                solicitacao.processoId(),
                solicitacao.oficialId(),
                solicitacao.destinatarioNome(),
                mascararDoc(solicitacao.destinatarioDocumento()),
                Instant.now(),
                solicitacao.latitude(),
                solicitacao.longitude(),
                solicitacao.enderecoConfirmado(),
                List.copyOf(solicitacao.evidencias()),
                solicitacao.fotoHashBase64(),
                solicitacao.biometriaHashOficial(),
                solicitacao.observacoes(),
                certidao,
                hashIntegridade,
                assinatura,
                true,
                false
        );
        persistirRegistro(registro);
        sincronizarExpedicao(recuperarExpedicao(solicitacao.mandadoId()).orElse(null), registro, hashIntegridade);
        auditLedger.appendSafely(
                "RECUSA_RECEBIMENTO_REGISTRADA",
                RESOURCE_TYPE,
                solicitacao.mandadoId(),
                hashIntegridade,
                "CPC art. 251 - recusa de recebimento. Citação efetivada. Evidências: %d".formatted(solicitacao.evidencias().size())
        );
        notificarJuizRecusa(registro);
        publicarWebhook(registro);
        log.info("[Recusa] Recusa registrada e citação efetivada. mandado={} destinatario={}", solicitacao.mandadoId(), mascararDoc(solicitacao.destinatarioDocumento()));
        return registro;
    }

    public Optional<RegistroRecusa> consultar(String mandadoId) {
        RegistroRecusa cache = registrosPorMandado.getIfPresent(mandadoId);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<RegistroRecusa> persisted = stateStore.find(DOMAIN_RECUSA, mandadoId, RegistroRecusa.class);
        persisted.ifPresent(registro -> registrosPorMandado.put(mandadoId, registro));
        return persisted;
    }



    private void persistirRegistro(RegistroRecusa registro) {
        registrosPorMandado.put(registro.mandadoId(), registro);
        stateStore.save(DOMAIN_RECUSA, registro.mandadoId(), registro.registroUuid(), registro, registro.processoId(), registro.mandadoId(), registro.mandadoId(), registro.publicadoNoDje() ? "PUBLICADO_DJE" : "REGISTRADO");
    }

    private void sincronizarExpedicao(ExpedicaoJudicial expedicao, RegistroRecusa registro, String hashIntegridade) {
        if (expedicao == null) {
            return;
        }
        expedicao.confirmarEntregaAutomatica(hashIntegridade, "OFICIAL_RECUSA_RECEBIMENTO");
        expedicao.registrarCienciaJudicial();
        expedicaoRepository.save(expedicao);
        iniciarPrazoERevelia(expedicao);
    }

    private void iniciarPrazoERevelia(ExpedicaoJudicial expedicao) {
        PrazoRespostaPosEntregaEngine prazoEngine = prazoProvider.getIfAvailable();
        if (prazoEngine == null) {
            return;
        }
        PrazoRespostaPosEntregaEngine.PrazoResposta prazo = prazoEngine.iniciarPrazoAposEntrega(
                expedicao.getExpedicaoUuid(),
                resolverTipoUsuario(expedicao),
                resolverTipoPrazo(expedicao)
        );
        ReveliaAutomaticaEngine reveliaEngine = reveliaProvider.getIfAvailable();
        if (reveliaEngine != null) {
            reveliaEngine.iniciarMonitoramento(expedicao.getExpedicaoUuid(), expedicao.getProcessoId(), prazo.vencimentoEm());
        }
    }

    private Optional<ExpedicaoJudicial> recuperarExpedicao(String mandadoId) {
        if (mandadoId == null || mandadoId.isBlank()) {
            return Optional.empty();
        }
        return expedicaoRepository.findByExpedicaoUuid(mandadoId);
    }

    private void publicarWebhook(RegistroRecusa registro) {
        WebhookOutboundService webhook = webhookProvider.getIfAvailable();
        if (webhook == null) {
            return;
        }
        webhook.publicar(new WebhookOutboundService.WebhookPayload(
                UUID.randomUUID().toString(),
                WebhookOutboundService.EventoWebhook.CITACAO_EFETIVADA,
                registro.processoId(),
                null,
                registro.mandadoId(),
                "RECUSA_RECEBIMENTO",
                Instant.now(),
                java.util.Map.of(
                        "oficialId", String.valueOf(registro.oficialId()),
                        "destinatario", String.valueOf(registro.destinatarioNome()),
                        "evidencias", String.valueOf(registro.evidencias().size())
                )
        ));
    }

    private void validar(SolicitacaoRecusa s) {
        if (s.mandadoId() == null || s.mandadoId().isBlank()) {
            throw new IllegalArgumentException("Mandado obrigatório para registro de recusa.");
        }
        if (s.latitude() == 0.0 && s.longitude() == 0.0) {
            throw new IllegalArgumentException("GPS obrigatório para registro de recusa.");
        }
        if (s.evidencias() == null || s.evidencias().isEmpty()) {
            throw new IllegalArgumentException("Ao menos uma evidência obrigatória para registro de recusa.");
        }
        boolean temEvidenciaForte = s.evidencias().stream().anyMatch(e -> e == TipoEvidenciaRecusa.FOTO_DESTINATARIO_PRESENTE
                || e == TipoEvidenciaRecusa.BIOMETRIA_OFICIAL
                || e == TipoEvidenciaRecusa.TESTEMUNHA_PRESENCIAL
                || e == TipoEvidenciaRecusa.VIDEO_CURTO);
        if (!temEvidenciaForte) {
            throw new IllegalArgumentException("Recusa exige ao menos uma evidência forte: foto, biometria, testemunha ou vídeo.");
        }
    }

    private String gerarCertidaoRecusa(SolicitacaoRecusa s, double distanciaMetros) {
        String evidenciasStr = s.evidencias().stream().map(Enum::name).reduce((a, b) -> a + " | " + b).orElse("NAO_INFORMADAS");
        return """
                CERTIDAO DE RECUSA DE RECEBIMENTO - CPC ART. 251
                Mandado: %s | Processo: %s
                Data/Hora: %s
                Destinatario: %s
                Endereco: %s | GPS: %.6f, %.6f | Distancia do alvo: %.2fm
                Evidencias documentadas: %s
                Observacoes: %s
                DECLARACAO: O destinatario acima qualificado, estando presente no local,
                recusou-se expressamente a receber o ato processual. Na forma do art. 251 do CPC,
                a citacao/intimacao considera-se realizada nesta data e hora, produzindo todos
                os efeitos legais a partir deste momento.
                """.formatted(
                s.mandadoId(),
                s.processoId(),
                Instant.now(),
                s.destinatarioNome(),
                s.enderecoConfirmado(),
                s.latitude(),
                s.longitude(),
                distanciaMetros,
                evidenciasStr,
                s.observacoes() != null ? s.observacoes() : "Sem observacoes."
        );
    }

    private void notificarJuizRecusa(RegistroRecusa registro) {
        processoRepository.findProcessoCompletoById(registro.processoId()).ifPresent(processo -> {
            if (processo.getUsuario() == null) {
                return;
            }
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    processo.getUsuario().getId(),
                    registro.processoId(),
                    NotificacaoInteligentePJB.TipoAlerta.MOVIMENTACAO_NOVA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.ALTA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }

    private NationalPrazoEngine.TipoPrazo resolverTipoPrazo(ExpedicaoJudicial expedicao) {
        if (expedicao.getRamoDireito() == null) {
            return NationalPrazoEngine.TipoPrazo.CONTESTACAO;
        }
        return switch (expedicao.getRamoDireito()) {
            case "PENAL", "MILITAR" -> NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL;
            case "TRABALHISTA" -> NationalPrazoEngine.TipoPrazo.RESPOSTA_TRABALHISTA;
            default -> NationalPrazoEngine.TipoPrazo.CONTESTACAO;
        };
    }

    private TipoUsuario resolverTipoUsuario(ExpedicaoJudicial expedicao) {
        return switch (expedicao.getTipoDestinatario()) {
            case DEFENSOR_PUBLICO -> TipoUsuario.DEFENSOR_PUBLICO;
            case FAZENDA_PUBLICA -> TipoUsuario.PROCURADORIA_FEDERAL;
            default -> TipoUsuario.CIDADAO;
        };
    }

    private static String sha256Hex(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(raw));
        }
    }

    private static String mascararDoc(String doc) {
        if (doc == null || doc.length() < 4) {
            return "***";
        }
        return doc.substring(0, 3) + "***" + doc.substring(Math.max(3, doc.length() - 2));
    }
}
