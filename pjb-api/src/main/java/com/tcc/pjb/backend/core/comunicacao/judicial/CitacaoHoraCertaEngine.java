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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CitacaoHoraCertaEngine {

    private static final Logger log = LoggerFactory.getLogger(CitacaoHoraCertaEngine.class);
    private static final String RESOURCE_TYPE = "HORA_CERTA";
    private static final String DOMAIN_TENTATIVA = "HORA_CERTA_TENTATIVA";
    private static final String DOMAIN_AGENDAMENTO = "HORA_CERTA_AGENDAMENTO";
    private static final String DOMAIN_RESULTADO = "HORA_CERTA_RESULTADO";
    private static final int MAX_TENTATIVAS_ANTES_HORA_CERTA = 2;

    public enum StatusHoraCerta {
        PRIMEIRA_TENTATIVA_REGISTRADA,
        SEGUNDA_TENTATIVA_REGISTRADA,
        HORA_CERTA_AGENDADA,
        AVISO_ENTREGUE,
        CITACAO_REALIZADA,
        FRUSTRADA_DEFINITIVA
    }

    public enum EvidenciaMorfologica {
        VIZINHO_CONFIRMOU_PRESENCA,
        VEICULO_NO_LOCAL,
        LUZ_ACESA,
        SOM_INTERNO_DETECTADO,
        MOVIMENTO_OBSERVADO,
        RECUSOU_ABERTURA,
        TRABALHADOR_CONFIRMOU_PRESENCA,
        PORTEIRO_CONFIRMOU_PRESENCA
    }

    public record TentativaHoraCerta(
            String mandadoId,
            Long processoId,
            Long oficialId,
            int numeroTentativa,
            Instant realizadaEm,
            double latitudeRealizada,
            double longitudeRealizada,
            String enderecoConfirmado,
            List<EvidenciaMorfologica> evidencias,
            String observacoes,
            boolean destinatarioAusente
    ) {
    }

    public record AgendamentoHoraCerta(
            String agendamentoUuid,
            String mandadoId,
            Long processoId,
            Long oficialId,
            Instant horaMarcadaEm,
            Instant horaMarcadaPara,
            String enderecoAgendado,
            String avisoEntregueA,
            boolean avisoConfirmado,
            String hashIntegridade
    ) {
    }

    public record ResultadoHoraCerta(
            String resultadoUuid,
            String mandadoId,
            Long processoId,
            StatusHoraCerta status,
            Instant realizadaEm,
            double latitudeRealizada,
            double longitudeRealizada,
            boolean destinatarioPresenteNaHoraMarcada,
            boolean citacaoEfetivada,
            String certidaoGerada,
            String hashIntegridade,
            PjbHardwareSecurityModule.AssinaturaHsm assinaturaHsm
    ) {
    }

    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLedgerService auditLedger;
    private final NotificacaoInteligentePJB notificacaoEngine;
    private final PjbHardwareSecurityModule hsm;
    private final GeofencePresencaOficialService geofenceService;
    private final ObjectProvider<WebhookOutboundService> webhookProvider;
    private final ObjectProvider<PrazoRespostaPosEntregaEngine> prazoProvider;
    private final ObjectProvider<ReveliaAutomaticaEngine> reveliaProvider;
    private final ObjectProvider<CuradorEspecialAutomaticoService> curadorProvider;
    private static final Duration CACHE_TTL = Duration.ofHours(12);

    private final ComunicacaoJudicialStateStore stateStore;
    private final Cache<String, List<TentativaHoraCerta>> tentativasPorMandado = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(CACHE_TTL)
            .build();
    private final Cache<String, AgendamentoHoraCerta> agendamentosPorMandado = Caffeine.newBuilder()
            .maximumSize(50000)
            .expireAfterAccess(CACHE_TTL)
            .build();

    public CitacaoHoraCertaEngine(ExpedicaoJudicialRepository expedicaoRepository,
                                  ProcessoRepository processoRepository,
                                  UsuarioRepository usuarioRepository,
                                  AuditLedgerService auditLedger,
                                  NotificacaoInteligentePJB notificacaoEngine,
                                  PjbHardwareSecurityModule hsm,
                                  GeofencePresencaOficialService geofenceService,
                                  ObjectProvider<WebhookOutboundService> webhookProvider,
                                  ObjectProvider<PrazoRespostaPosEntregaEngine> prazoProvider,
                                  ObjectProvider<ReveliaAutomaticaEngine> reveliaProvider,
                                  ObjectProvider<CuradorEspecialAutomaticoService> curadorProvider,
                                  ComunicacaoJudicialStateStore stateStore) {
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.notificacaoEngine = Objects.requireNonNull(notificacaoEngine, "notificacaoEngine");
        this.hsm = Objects.requireNonNull(hsm, "hsm");
        this.geofenceService = Objects.requireNonNull(geofenceService, "geofenceService");
        this.webhookProvider = Objects.requireNonNull(webhookProvider, "webhookProvider");
        this.prazoProvider = Objects.requireNonNull(prazoProvider, "prazoProvider");
        this.reveliaProvider = Objects.requireNonNull(reveliaProvider, "reveliaProvider");
        this.curadorProvider = Objects.requireNonNull(curadorProvider, "curadorProvider");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
    }

    @Transactional
    public Optional<AgendamentoHoraCerta> registrarTentativa(TentativaHoraCerta tentativa) {
        Objects.requireNonNull(tentativa, "tentativa");
        validarGeoFence(tentativa);
        List<TentativaHoraCerta> historico = new ArrayList<>(consultarTentativas(tentativa.mandadoId()));
        boolean duplicata = historico.stream().anyMatch(t -> t.numeroTentativa() == tentativa.numeroTentativa());
        if (duplicata) {
            throw new IllegalStateException("Tentativa %d ja registrada para mandado %s.".formatted(tentativa.numeroTentativa(), tentativa.mandadoId()));
        }
        historico.add(tentativa);
        persistirTentativa(tentativa);
        auditLedger.appendSafely(
                "HORA_CERTA_TENTATIVA_" + tentativa.numeroTentativa(),
                RESOURCE_TYPE,
                tentativa.mandadoId(),
                sha256Hex(tentativa.toString()),
                "Tentativa %d - evidencias: %s".formatted(
                        tentativa.numeroTentativa(),
                        tentativa.evidencias().stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse("NENHUMA")
                )
        );
        if (historico.size() >= MAX_TENTATIVAS_ANTES_HORA_CERTA && configuracaoAdmiteHoraCerta(tentativa)) {
            AgendamentoHoraCerta agendamento = gerarAgendamento(tentativa);
            persistirAgendamento(agendamento);
            publicarWebhookAgendamento(agendamento);
            notificarOficialAgendamento(agendamento);
            log.info("[HoraCerta] Agendamento gerado. mandado={} horaPara={}", tentativa.mandadoId(), agendamento.horaMarcadaPara());
            return Optional.of(agendamento);
        }
        return Optional.empty();
    }

    @Transactional
    public ResultadoHoraCerta executarHoraCerta(String mandadoId,
                                                Long oficialId,
                                                double latitude,
                                                double longitude,
                                                boolean destinatarioPresente,
                                                String observacoes) {
        Objects.requireNonNull(mandadoId, "mandadoId");
        AgendamentoHoraCerta agendamento = consultarAgendamento(mandadoId).orElse(null);
        if (agendamento == null) {
            throw new IllegalStateException("Nenhum agendamento de hora certa encontrado para mandado: " + mandadoId);
        }
        GeofencePresencaOficialService.ValidacaoPresenca validacao = geofenceService.validar(mandadoId, oficialId, latitude, longitude);
        if (!validacao.liberadoParaRegistro()) {
            throw new IllegalStateException("GPS invalido para execucao de hora certa do mandado: " + mandadoId);
        }
        validarJanelaTemporalHoraCerta(agendamento);
        String certidao = gerarCertidaoHoraCerta(agendamento, destinatarioPresente, observacoes, latitude, longitude);
        byte[] certidaoBytes = certidao.getBytes(StandardCharsets.UTF_8);
        PjbHardwareSecurityModule.AssinaturaHsm assinatura = hsm.assinar(certidaoBytes);
        String hashIntegridade = sha256Hex(mandadoId + latitude + longitude + Instant.now().toEpochMilli());
        ResultadoHoraCerta resultado = new ResultadoHoraCerta(
                UUID.randomUUID().toString(),
                mandadoId,
                agendamento.processoId(),
                StatusHoraCerta.CITACAO_REALIZADA,
                Instant.now(),
                latitude,
                longitude,
                destinatarioPresente,
                true,
                certidao,
                hashIntegridade,
                assinatura
        );
        sincronizarExpedicao(mandadoId, agendamento, destinatarioPresente, hashIntegridade);
        auditLedger.appendSafely(
                "HORA_CERTA_EXECUTADA",
                RESOURCE_TYPE,
                mandadoId,
                hashIntegridade,
                "Hora certa executada. Presente=%s. Certidao gerada.".formatted(destinatarioPresente)
        );
        log.info("[HoraCerta] Citacao por hora certa efetivada. mandado={} presente={}", mandadoId, destinatarioPresente);
        stateStore.save(DOMAIN_RESULTADO, resultado.mandadoId(), resultado.resultadoUuid(), resultado, resultado.processoId(), resultado.mandadoId(), resultado.mandadoId(), resultado.status().name());
        return resultado;
    }

    public Optional<AgendamentoHoraCerta> consultarAgendamento(String mandadoId) {
        AgendamentoHoraCerta cache = agendamentosPorMandado.getIfPresent(mandadoId);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<AgendamentoHoraCerta> persisted = stateStore.find(DOMAIN_AGENDAMENTO, mandadoId, AgendamentoHoraCerta.class);
        persisted.ifPresent(agendamento -> agendamentosPorMandado.put(mandadoId, agendamento));
        return persisted;
    }

    public List<TentativaHoraCerta> consultarTentativas(String mandadoId) {
        List<TentativaHoraCerta> cache = tentativasPorMandado.getIfPresent(mandadoId);
        if (cache != null && !cache.isEmpty()) {
            return List.copyOf(cache);
        }
        List<TentativaHoraCerta> persisted = stateStore.findBySecondaryKey(DOMAIN_TENTATIVA, mandadoId, TentativaHoraCerta.class);
        if (!persisted.isEmpty()) {
            tentativasPorMandado.put(mandadoId, List.copyOf(persisted));
        }
        return List.copyOf(persisted);
    }

    @PjbClusterSingletonTask(key = "citacao-hora-certa-vencidos", ttl = "PT4M")
    @Scheduled(fixedDelay = 900_000)
    public void monitorarAgendamentosVencidos() {
        Instant agora = Instant.now();
        agendamentosPendentes().stream()
                .filter(a -> a.horaMarcadaPara().isBefore(agora.minus(2, ChronoUnit.HOURS)))
                .forEach(a -> {
                    log.warn("[HoraCerta] Agendamento vencido sem execucao registrada. mandado={}", a.mandadoId());
                    auditLedger.appendSafely(
                            "HORA_CERTA_VENCIDA_SEM_EXECUCAO",
                            RESOURCE_TYPE,
                            a.mandadoId(),
                            sha256Hex(a.agendamentoUuid()),
                            "Hora certa vencida. Requer verificacao manual."
                    );
                });
    }

    private void sincronizarExpedicao(String mandadoId,
                                      AgendamentoHoraCerta agendamento,
                                      boolean destinatarioPresente,
                                      String hashIntegridade) {
        ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(mandadoId).orElse(null);
        if (expedicao == null) {
            return;
        }
        expedicao.confirmarEntregaAutomatica(hashIntegridade, "OFICIAL_HORA_CERTA");
        expedicao.registrarCienciaJudicial();
        expedicaoRepository.save(expedicao);
        iniciarPrazoERevelia(expedicao);
        if (!destinatarioPresente) {
            CuradorEspecialAutomaticoService curador = curadorProvider.getIfAvailable();
            if (curador != null) {
                curador.registrarNecessidadeSeAusente(
                        agendamento.processoId(),
                        mandadoId,
                        CuradorEspecialAutomaticoService.TipoCuradoria.CITADO_POR_HORA_CERTA
                );
            }
        }
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

    private void publicarWebhookAgendamento(AgendamentoHoraCerta agendamento) {
        WebhookOutboundService webhook = webhookProvider.getIfAvailable();
        if (webhook == null) {
            return;
        }
        webhook.publicar(new WebhookOutboundService.WebhookPayload(
                UUID.randomUUID().toString(),
                WebhookOutboundService.EventoWebhook.HORA_CERTA_AGENDADA,
                agendamento.processoId(),
                null,
                agendamento.mandadoId(),
                StatusHoraCerta.HORA_CERTA_AGENDADA.name(),
                Instant.now(),
                java.util.Map.of(
                        "horaMarcadaPara", agendamento.horaMarcadaPara().toString(),
                        "endereco", String.valueOf(agendamento.enderecoAgendado())
                )
        ));
    }

    private AgendamentoHoraCerta gerarAgendamento(TentativaHoraCerta ultimaTentativa) {
        Instant horaPara = Instant.now().plus(24, ChronoUnit.HOURS);
        String uuid = UUID.randomUUID().toString();
        String hash = sha256Hex(uuid + ultimaTentativa.mandadoId() + horaPara.toEpochMilli());
        return new AgendamentoHoraCerta(
                uuid,
                ultimaTentativa.mandadoId(),
                ultimaTentativa.processoId(),
                ultimaTentativa.oficialId(),
                Instant.now(),
                horaPara,
                ultimaTentativa.enderecoConfirmado(),
                "VIZINHO_OU_FAMILIAR",
                false,
                hash
        );
    }

    private void persistirTentativa(TentativaHoraCerta tentativa) {
        List<TentativaHoraCerta> atuais = tentativasPorMandado.getIfPresent(tentativa.mandadoId());
        List<TentativaHoraCerta> atualizadas = atuais == null ? new ArrayList<>() : new ArrayList<>(atuais);
        atualizadas.add(tentativa);
        tentativasPorMandado.put(tentativa.mandadoId(), List.copyOf(atualizadas));
        stateStore.save(DOMAIN_TENTATIVA, tentativa.mandadoId() + ':' + tentativa.numeroTentativa(), tentativa.mandadoId(), tentativa, tentativa.processoId(), tentativa.mandadoId(), tentativa.mandadoId(), String.valueOf(tentativa.numeroTentativa()));
    }

    private void persistirAgendamento(AgendamentoHoraCerta agendamento) {
        agendamentosPorMandado.put(agendamento.mandadoId(), agendamento);
        stateStore.save(DOMAIN_AGENDAMENTO, agendamento.mandadoId(), agendamento.agendamentoUuid(), agendamento, agendamento.processoId(), agendamento.mandadoId(), agendamento.mandadoId(), agendamento.avisoConfirmado() ? "AVISO_CONFIRMADO" : "AGENDADO");
    }

    private List<AgendamentoHoraCerta> agendamentosPendentes() {
        Map<String, AgendamentoHoraCerta> consolidados = new LinkedHashMap<>();
        stateStore.findByStatusCodes(
                DOMAIN_AGENDAMENTO,
                List.of("AGENDADO", "AVISO_CONFIRMADO"),
                AgendamentoHoraCerta.class
        ).forEach(agendamento -> consolidados.put(agendamento.mandadoId(), agendamento));
        agendamentosPorMandado.asMap().values().forEach(agendamento -> consolidados.put(agendamento.mandadoId(), agendamento));
        return List.copyOf(consolidados.values());
    }

    private boolean configuracaoAdmiteHoraCerta(TentativaHoraCerta tentativa) {
        return tentativa.destinatarioAusente()
                && tentativa.evidencias() != null
                && !tentativa.evidencias().isEmpty()
                && tentativa.evidencias().stream().anyMatch(e -> e == EvidenciaMorfologica.VIZINHO_CONFIRMOU_PRESENCA
                || e == EvidenciaMorfologica.PORTEIRO_CONFIRMOU_PRESENCA
                || e == EvidenciaMorfologica.VEICULO_NO_LOCAL
                || e == EvidenciaMorfologica.LUZ_ACESA);
    }

    private void validarGeoFence(TentativaHoraCerta tentativa) {
        GeofencePresencaOficialService.ValidacaoPresenca validacao = geofenceService.validar(
                tentativa.mandadoId(),
                tentativa.oficialId(),
                tentativa.latitudeRealizada(),
                tentativa.longitudeRealizada()
        );
        if (!validacao.liberadoParaRegistro()) {
            throw new IllegalArgumentException("Coordenadas GPS obrigatorias e dentro do raio para registro de tentativa.");
        }
    }

    private void validarJanelaTemporalHoraCerta(AgendamentoHoraCerta agendamento) {
        Instant agora = Instant.now();
        if (agora.isBefore(agendamento.horaMarcadaPara().minus(30, ChronoUnit.MINUTES))) {
            throw new IllegalStateException("Hora certa so pode ser executada nos 30 minutos anteriores ou apos a hora marcada.");
        }
    }

    private String gerarCertidaoHoraCerta(AgendamentoHoraCerta agendamento,
                                          boolean presente,
                                          String observacoes,
                                          double lat,
                                          double lon) {
        return """
                CERTIDAO DE HORA CERTA - CPC ART. 252-254
                Mandado: %s | Processo: %s
                Hora marcada em: %s | Realizada em: %s
                Endereco: %s | GPS: %.6f, %.6f
                Destinatario presente na hora marcada: %s
                Observacoes: %s
                Fundamento: CPC art. 252 - citacao por hora certa efetivada com presuncao legal de ciencia.
                """.formatted(
                agendamento.mandadoId(),
                agendamento.processoId(),
                agendamento.horaMarcadaEm(),
                Instant.now(),
                agendamento.enderecoAgendado(),
                lat,
                lon,
                presente ? "SIM" : "NAO (citacao efetivada por forca do art. 253 CPC)",
                observacoes != null ? observacoes : "Sem observacoes adicionais."
        );
    }

    private void notificarOficialAgendamento(AgendamentoHoraCerta agendamento) {
        usuarioRepository.findById(agendamento.oficialId()).ifPresent(oficial -> {
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    oficial.getId(),
                    agendamento.processoId(),
                    NotificacaoInteligentePJB.TipoAlerta.AUDIENCIA_AMANHA,
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
}
