package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import com.tcc.pjb.backend.platform.cluster.PjbClusterSingletonTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.AlvoJuridico;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.MotorInterceptacaoAtiva;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.ReciboCitacaoHsm;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.SefazNfeCadastroResolver;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.ViaInterceptacao;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.institutional.movimentacao.MovimentacaoProcessualRegistrar;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;

@Service
public class CitacaoIntimacaoEngine {

    private static final Logger log = LoggerFactory.getLogger(CitacaoIntimacaoEngine.class);
    private static final String RESOURCE_TYPE = "EXPEDICAO_JUDICIAL";
    private static final int MAX_TENTATIVAS = 3;
    private static final long JANELA_EVASAO_DIAS = 30L;

    public sealed interface PerfilDestinatario permits
            PerfilDestinatario.PessoaFisica,
            PerfilDestinatario.PessoaJuridica,
            PerfilDestinatario.AdvogadoOab,
            PerfilDestinatario.DefensorPublico,
            PerfilDestinatario.MinisterioPublico,
            PerfilDestinatario.FazendaPublica,
            PerfilDestinatario.JuizoDeprecado {

        record PessoaFisica(
                String cpf,
                String nome,
                String govbrAccountId,
                String email,
                String telefone,
                boolean possuiContaGovBr,
                boolean possuiAdvogado
        ) implements PerfilDestinatario {
        }

        record PessoaJuridica(
                String cnpj,
                String razaoSocial,
                String emailReceita,
                String telefoneReceita,
                String enderecoSede,
                boolean possuiPortalGovBr,
                boolean isGrandeEmpresa,
                boolean isBanco,
                boolean isFazendaPublica,
                boolean cnpjAtivo
        ) implements PerfilDestinatario {
        }

        record AdvogadoOab(
                String cpf,
                String oabNumero,
                String uf,
                String emailOab,
                boolean cadastradoSistemaJudicial,
                String sistemaPrincipal
        ) implements PerfilDestinatario {
        }

        record DefensorPublico(
                String cpf,
                String funcionalDefensoria,
                String emailInstitucional
        ) implements PerfilDestinatario {
        }

        record MinisterioPublico(
                String cpf,
                String funcionalMp,
                String emailInstitucional
        ) implements PerfilDestinatario {
        }

        record FazendaPublica(
                String cnpj,
                String razaoSocial,
                String emailPgfn,
                String ente
        ) implements PerfilDestinatario {
        }

        record JuizoDeprecado(
                String codigoTribunal,
                String comarca,
                String uf,
                String emailCarta
        ) implements PerfilDestinatario {
        }
    }

    public record ExpedicaoRequest(
            Long processoId,
            TipoComunicacaoJudicial tipoComunicacao,
            PerfilDestinatario destinatario,
            String conteudoDoAto,
            String fundamentoAdicional,
            boolean forcarDigital,
            boolean forcarOficial,
            Long juizResponsavelId,
            Long servidorExpedidorId
    ) {
    }

    public record ExpedicaoResponse(
            String expedicaoUuid,
            Long processoId,
            TipoComunicacaoJudicial tipoComunicacao,
            ModalidadeExpedicaoJudicial modalidadeEscolhida,
            ExpedicaoJudicial.StatusExpedicao status,
            String destinatarioDocumento,
            String destinatarioNome,
            String canalDigitalUtilizado,
            Instant expedidaEm,
            Instant presuncaoEntregaEm,
            List<String> alertas,
            List<String> cascataModalidades,
            String hashIntegridade,
            String fundamentacaoLegal,
            boolean antiEvasaoAtivado
    ) {
    }

    public record AcuseRecebimentoRequest(
            String expedicaoUuid,
            String tokenAcuse,
            String ipOrigem,
            String deviceFingerprint,
            String govbrSessionToken
    ) {
    }

    public record EvasaoRelatorio(
            String documento,
            long totalFrustracoes,
            long totalExpedicoes,
            double taxaFrustracao,
            boolean evasaoConfirmada,
            List<String> evidencias,
            String recomendacao
    ) {
    }

    public record PainelExpedicoes(
            long totalExpedidas,
            long totalEntregues,
            long totalPresumidas,
            long totalFrustradas,
            long totalPendentesOficial,
            long totalEvasoes,
            long totalEscalonadas,
            Instant geradoEm,
            String hashIntegridade
    ) {
    }

    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLedgerService auditLedger;
    private final CurrentUserService currentUserService;
    private final NotificacaoInteligentePJB notificacaoEngine;
    private final ObjectProvider<MotorInterceptacaoAtiva> motorInterceptacaoProvider;
    private final ObjectProvider<WebhookOutboundService> webhookProvider;
    private final ObjectProvider<PrazoRespostaPosEntregaEngine> prazoProvider;
    private final ObjectProvider<ReveliaAutomaticaEngine> reveliaProvider;
    private final ObjectProvider<CuradorEspecialAutomaticoService> curadorProvider;
    private final ObjectProvider<QrCodeMandadoService> qrCodeProvider;
    private final ObjectProvider<ComunicacaoJudicialPortalNotificationService> portalNotificationProvider;
    private final ObjectProvider<ComunicacaoJudicialAtendimentoRelayService> atendimentoRelayProvider;
    private final ObjectProvider<SefazNfeCadastroResolver> sefazCadastroResolverProvider;
    private final MatrizComunicacaoJudicialResolver matrizResolver;
    private final PjbHsmProperties hsmProperties;
    private final PjbExecutionOrchestrator executionOrchestrator;
    private final MovimentacaoProcessualRegistrar movimentacaoRegistrar;

    public CitacaoIntimacaoEngine(ExpedicaoJudicialRepository expedicaoRepository,
                                  ProcessoRepository processoRepository,
                                  UsuarioRepository usuarioRepository,
                                  AuditLedgerService auditLedger,
                                  CurrentUserService currentUserService,
                                  NotificacaoInteligentePJB notificacaoEngine,
                                  ObjectProvider<MotorInterceptacaoAtiva> motorInterceptacaoProvider,
                                  ObjectProvider<WebhookOutboundService> webhookProvider,
                                  ObjectProvider<PrazoRespostaPosEntregaEngine> prazoProvider,
                                  ObjectProvider<ReveliaAutomaticaEngine> reveliaProvider,
                                  ObjectProvider<CuradorEspecialAutomaticoService> curadorProvider,
                                  ObjectProvider<QrCodeMandadoService> qrCodeProvider,
                                  ObjectProvider<ComunicacaoJudicialPortalNotificationService> portalNotificationProvider,
                                  ObjectProvider<ComunicacaoJudicialAtendimentoRelayService> atendimentoRelayProvider,
                                  ObjectProvider<SefazNfeCadastroResolver> sefazCadastroResolverProvider,
                                  MatrizComunicacaoJudicialResolver matrizResolver,
                                  PjbHsmProperties hsmProperties,
                                  PjbExecutionOrchestrator executionOrchestrator,
                                  MovimentacaoProcessualRegistrar movimentacaoRegistrar) {
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.notificacaoEngine = Objects.requireNonNull(notificacaoEngine, "notificacaoEngine");
        this.motorInterceptacaoProvider = Objects.requireNonNull(motorInterceptacaoProvider, "motorInterceptacaoProvider");
        this.webhookProvider = Objects.requireNonNull(webhookProvider, "webhookProvider");
        this.prazoProvider = Objects.requireNonNull(prazoProvider, "prazoProvider");
        this.reveliaProvider = Objects.requireNonNull(reveliaProvider, "reveliaProvider");
        this.curadorProvider = Objects.requireNonNull(curadorProvider, "curadorProvider");
        this.qrCodeProvider = Objects.requireNonNull(qrCodeProvider, "qrCodeProvider");
        this.portalNotificationProvider = Objects.requireNonNull(portalNotificationProvider, "portalNotificationProvider");
        this.atendimentoRelayProvider = Objects.requireNonNull(atendimentoRelayProvider, "atendimentoRelayProvider");
        this.sefazCadastroResolverProvider = Objects.requireNonNull(sefazCadastroResolverProvider, "sefazCadastroResolverProvider");
        this.matrizResolver = Objects.requireNonNull(matrizResolver, "matrizResolver");
        this.hsmProperties = Objects.requireNonNull(hsmProperties, "hsmProperties");
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
        this.movimentacaoRegistrar = Objects.requireNonNull(movimentacaoRegistrar, "movimentacaoRegistrar");
    }

    @Transactional
    public ExpedicaoResponse expedir(ExpedicaoRequest request) {
        validarRequest(request);
        Processo processo = processoRepository.findProcessoCompletoById(request.processoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + request.processoId()));
        RamoDireito ramo = processo.getRamoDireito();
        if (ramo != null && !request.tipoComunicacao().isAplicavelAo(ramo)) {
            throw new IllegalArgumentException(
                    "Tipo de comunicação " + request.tipoComunicacao().name() + " não é aplicável ao ramo " + ramo.name()
            );
        }
        List<String> alertas = new ArrayList<>();
        List<String> cascata = new ArrayList<>();
        ModalidadeExpedicaoJudicial modalidade = resolverModalidadeOtima(request, processo, alertas, cascata);
        String documento = CitacaoIntimacaoExpedicaoSupport.extrairDocumento(request.destinatario());
        String nome = CitacaoIntimacaoExpedicaoSupport.extrairNome(request.destinatario());
        String hashAnterior = recuperarHashAnterior(documento, request.processoId());
        String fundamentacao = CitacaoIntimacaoExpedicaoSupport.montarFundamentacao(request, processo, modalidade);
        String hashIntegridade = CitacaoIntimacaoExpedicaoSupport.computarHash(
                request.processoId(),
                documento,
                modalidade,
                request.tipoComunicacao(),
                Instant.now(),
                hashAnterior
        );
        ExpedicaoJudicial.TipoDestinatario tipoDestinatario = CitacaoIntimacaoExpedicaoSupport.resolverTipoDestinatario(request.destinatario());
        GrauJurisdicao grau = processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : GrauJurisdicao.PRIMEIRO_GRAU;
        ExpedicaoJudicial expedicao = new ExpedicaoJudicial(
                request.processoId(),
                processo.getNumeroUnificado(),
                request.tipoComunicacao(),
                modalidade,
                tipoDestinatario,
                nome,
                documento,
                ramo != null ? ramo.name() : null,
                grau != null ? grau.name() : null,
                hashAnterior,
                hashIntegridade,
                fundamentacao
        );
        enriquecerExpedicao(expedicao, request, processo);
        long frustracoesRecentes = expedicaoRepository.contarFrustracoesRecentes(
                documento,
                Instant.now().minus(JANELA_EVASAO_DIAS, ChronoUnit.DAYS)
        );
        boolean antiEvasaoAtivado = false;
        if (frustracoesRecentes >= 2) {
            String evidencias = "Documento %s apresenta %d frustrações nos últimos %d dias.".formatted(
                    CitacaoIntimacaoExpedicaoSupport.mascararDocumento(documento),
                    frustracoesRecentes,
                    JANELA_EVASAO_DIAS
            );
            expedicao.registrarEvasao(evidencias);
            alertas.add("Anti-evasão ativado: " + evidencias);
            antiEvasaoAtivado = true;
        }
        ExpedicaoJudicial salva = expedicaoRepository.save(expedicao);
        notificarPortalDestinatario(salva, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA);
        propagarAvisoAtendimento(salva, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.EXPEDIDA);
        publicarWebhookExpedicao(salva, WebhookOutboundService.EventoWebhook.EXPEDICAO_EXPEDIDA, java.util.Map.of("fundamento", salva.getFundamentacaoLegal()));
        agendarDespachoAposCommit(salva.getExpedicaoUuid(), request, processo.getId(), modalidade);
        auditLedger.appendSafely(
                "EXPEDICAO_JUDICIAL_EXPEDIDA",
                RESOURCE_TYPE,
                salva.getExpedicaoUuid(),
                hashIntegridade,
                fundamentacao
        );
        log.info(
                "[CitacaoEngine] Expedida uuid={} processo={} tipo={} modalidade={} destinatario={}",
                salva.getExpedicaoUuid(),
                request.processoId(),
                request.tipoComunicacao(),
                modalidade,
                CitacaoIntimacaoExpedicaoSupport.mascararDocumento(documento)
        );
        return new ExpedicaoResponse(
                salva.getExpedicaoUuid(),
                salva.getProcessoId(),
                salva.getTipoComunicacao(),
                salva.getModalidade(),
                salva.getStatus(),
                CitacaoIntimacaoExpedicaoSupport.mascararDocumento(salva.getDestinatarioDocumento()),
                salva.getDestinatarioNome(),
                salva.getCanalDigitalUtilizado(),
                salva.getExpedidaEm(),
                salva.getPresuncaoEntregaEm(),
                List.copyOf(alertas),
                List.copyOf(cascata),
                salva.getHashIntegridade(),
                salva.getFundamentacaoLegal(),
                antiEvasaoAtivado
        );
    }

    @Transactional
    public void processarAcuseRecebimento(AcuseRecebimentoRequest acuse) {
        Objects.requireNonNull(acuse, "acuse");
        ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(acuse.expedicaoUuid())
                .orElseThrow(() -> new IllegalArgumentException("Expedição não encontrada: " + acuse.expedicaoUuid()));
        String acuseHash = CitacaoIntimacaoExpedicaoSupport.computarHashAcuse(acuse);
        expedicao.confirmarEntrega(acuseHash, acuse.ipOrigem(), acuse.deviceFingerprint(), acuse.govbrSessionToken());
        expedicaoRepository.save(expedicao);
        Processo processo = processoRepository.findProcessoCompletoById(expedicao.getProcessoId()).orElse(null);
        iniciarPrazoEReveliaSeCabivel(expedicao);
        registrarMovimentacaoAcuse(processo, "Ciência da expedição confirmada pelo destinatário (acuse de recebimento).");
        notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.ENTREGUE_CONFIRMADA);
        propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.ENTREGUE_CONFIRMADA);
        publicarWebhookExpedicao(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_ENTREGUE_CONFIRMADA, java.util.Map.of("canal", String.valueOf(expedicao.getModalidade())));
        auditLedger.appendSafely(
                "EXPEDICAO_ENTREGA_CONFIRMADA",
                RESOURCE_TYPE,
                expedicao.getExpedicaoUuid(),
                acuseHash,
                "Entrega confirmada via " + expedicao.getModalidade()
        );
    }

    @Transactional
    public void processarConfirmacaoLeitura(String expedicaoUuid, String acuseHash) {
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Expedição não encontrada: " + expedicaoUuid));
        expedicao.confirmarLeitura(Instant.now(), acuseHash);
        expedicaoRepository.save(expedicao);
        Processo processo = processoRepository.findProcessoCompletoById(expedicao.getProcessoId()).orElse(null);
        iniciarPrazoEReveliaSeCabivel(expedicao);
        registrarMovimentacaoAcuse(processo, "Leitura da expedição confirmada pelo destinatário.");
        notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.LIDA_CONFIRMADA);
        propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.LIDA_CONFIRMADA);
        publicarWebhookExpedicao(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_LIDA_CONFIRMADA, java.util.Map.of("acuseHash", String.valueOf(acuseHash)));
        auditLedger.appendSafely(
                "EXPEDICAO_LEITURA_CONFIRMADA",
                RESOURCE_TYPE,
                expedicao.getExpedicaoUuid(),
                acuseHash,
                "Leitura confirmada. Prazo de resposta iniciado em " + expedicao.getPrazoRespostaInicioEm()
        );
    }

    private void registrarMovimentacaoAcuse(Processo processo, String descricao) {
        if (processo == null) {
            return;
        }
        Usuario ator = currentUserService.getOrNull();
        if (ator == null) {
            return;
        }
        movimentacaoRegistrar.registrar(processo, ator, processo.getFaseAtual(), descricao);
    }

    @Transactional
    public ExpedicaoResponse registrarFrustracaoEAcionarFallback(String expedicaoUuid, String motivoFrustracao) {
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Expedição não encontrada: " + expedicaoUuid));
        Processo processo = processoRepository.findProcessoCompletoById(expedicao.getProcessoId())
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + expedicao.getProcessoId()));
        ModalidadeExpedicaoJudicial fallback = escolherFallback(expedicao.getModalidade(), expedicao, processo);
        expedicao.registrarFrustracao(motivoFrustracao, fallback);
        if (fallback != null && expedicao.getTentativasRealizadas() < MAX_TENTATIVAS) {
            expedicao.incrementarTentativa(Instant.now().plus(1, ChronoUnit.HOURS));
            expedicao.setModalidade(fallback);
            expedicao.setStatus(CitacaoIntimacaoExpedicaoSupport.statusParaFallback(fallback));
            agendarDespachoAposCommit(expedicao.getExpedicaoUuid(), CitacaoIntimacaoExpedicaoSupport.rebuildRequest(expedicao, processo), processo.getId(), fallback);
        }
        long frustracoesTotal = expedicaoRepository.contarFrustracoesRecentes(
                expedicao.getDestinatarioDocumento(),
                Instant.now().minus(JANELA_EVASAO_DIAS, ChronoUnit.DAYS)
        );
        if (frustracoesTotal >= 2 && !expedicao.isEvasaoDetectada()) {
            expedicao.registrarEvasao("Padrão de não-recebimento detectado após %d frustrações.".formatted(frustracoesTotal));
        }
        if (expedicao.isEvasaoDetectada() && !expedicao.isEscalonadoParaJuiz()) {
            expedicao.escalonarParaJuiz();
            notificarJuizEvasao(expedicao, processo);
        }
        expedicaoRepository.save(expedicao);
        publicarWebhookExpedicao(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_FRUSTRADA, java.util.Map.of("motivo", String.valueOf(motivoFrustracao), "fallback", fallback != null ? fallback.name() : "NENHUM"));
        auditLedger.appendSafely(
                "EXPEDICAO_FRUSTRACAO_FALLBACK",
                RESOURCE_TYPE,
                expedicaoUuid,
                expedicao.getHashIntegridade(),
                "Frustração: " + motivoFrustracao + " | Fallback: " + (fallback != null ? fallback : "NENHUM")
        );
        return CitacaoIntimacaoExpedicaoSupport.buildResponseFrom(
                expedicao,
                List.of("Frustração registrada. Fallback: " + (fallback != null ? fallback.getLabel() : "Edital requerido."))
        );
    }

    @Transactional
    public void registrarEntregaPorInterceptacao(String expedicaoUuid, ReciboCitacaoHsm recibo) {
        Objects.requireNonNull(expedicaoUuid, "expedicaoUuid");
        Objects.requireNonNull(recibo, "recibo");
        if (!recibo.foiEntregue()) {
            return;
        }
        ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Expedição não encontrada: " + expedicaoUuid));
        expedicao.confirmarEntregaAutomatica(recibo.hashPayload(), recibo.canalVencedor());
        expedicaoRepository.save(expedicao);
        Processo processo = processoRepository.findProcessoCompletoById(expedicao.getProcessoId()).orElse(null);
        iniciarPrazoEReveliaSeCabivel(expedicao);
        notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.ENTREGUE_CONFIRMADA);
        propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.ENTREGUE_CONFIRMADA);
        publicarWebhookExpedicao(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_ENTREGUE_CONFIRMADA, java.util.Map.of("canal", recibo.canalVencedor()));
        auditLedger.appendSafely(
                "EXPEDICAO_INTERCEPTACAO_CONFIRMADA",
                RESOURCE_TYPE,
                expedicaoUuid,
                recibo.hashPayload(),
                "Entrega automática confirmada via interceptação ativa em " + recibo.canalVencedor()
        );
    }

    @Transactional(readOnly = true)
    public EvasaoRelatorio analisarEvasao(String documento) {
        String documentoNormalizado = CitacaoIntimacaoExpedicaoSupport.normalizarDocumento(documento);
        long frustracoes = expedicaoRepository.contarFrustracoesRecentes(
                documentoNormalizado,
                Instant.now().minus(JANELA_EVASAO_DIAS, ChronoUnit.DAYS)
        );
        List<ExpedicaoJudicial> historico = expedicaoRepository
                .findByDestinatarioDocumentoAndStatusNotOrderByExpedidaEmDesc(
                        documentoNormalizado,
                        ExpedicaoJudicial.StatusExpedicao.CANCELADA
                );
        long total = historico.size();
        double taxa = total == 0 ? 0.0 : (double) frustracoes / total;
        boolean evasaoConfirmada = frustracoes >= 2 && taxa >= 0.5d;
        List<String> evidencias = new ArrayList<>();
        if (frustracoes >= 2) {
            evidencias.add("%d frustrações nos últimos %d dias.".formatted(frustracoes, JANELA_EVASAO_DIAS));
        }
        historico.stream()
                .filter(e -> e.getMotivoFrustracao() != null)
                .limit(3)
                .forEach(e -> evidencias.add(e.getMotivoFrustracao()));
        String recomendacao = switch ((int) Math.min(frustracoes, 4)) {
            case 0, 1 -> "Aguardar resultado das tentativas em curso.";
            case 2 -> "Acionar Oficial de Justiça com mandado expedito.";
            case 3 -> "Escalonar ao juízo para decisão sobre edital.";
            default -> "Citação editalícia imediata com publicação no DOU/DJE. Possível revelia.";
        };
        return new EvasaoRelatorio(
                CitacaoIntimacaoExpedicaoSupport.mascararDocumento(documentoNormalizado),
                frustracoes,
                total,
                taxa,
                evasaoConfirmada,
                List.copyOf(evidencias),
                recomendacao
        );
    }

    @Transactional(readOnly = true)
    public PainelExpedicoes gerarPainel() {
        long expedidas = expedicaoRepository.countByStatus(ExpedicaoJudicial.StatusExpedicao.EXPEDIDA);
        long entregues = expedicaoRepository.countByStatusIn(EnumSet.of(
                ExpedicaoJudicial.StatusExpedicao.ENTREGUE_CONFIRMADA,
                ExpedicaoJudicial.StatusExpedicao.LIDA_CONFIRMADA
        ));
        long presumidas = expedicaoRepository.countByStatus(ExpedicaoJudicial.StatusExpedicao.PRESUMIDA_ENTREGUE);
        long frustradas = expedicaoRepository.countByStatus(ExpedicaoJudicial.StatusExpedicao.FRUSTRADA_DEFINITIVA);
        long pendOficial = expedicaoRepository.countByStatus(ExpedicaoJudicial.StatusExpedicao.PENDENTE_OFICIAL);
        long evasoes = expedicaoRepository.countByEvasaoDetectadaTrue();
        long escalonadas = expedicaoRepository.countByEscalonadoParaJuizTrue();
        String hash = CitacaoIntimacaoExpedicaoSupport.computarHashPainel(expedidas, entregues, presumidas, frustradas, pendOficial, evasoes, escalonadas);
        return new PainelExpedicoes(
                expedidas,
                entregues,
                presumidas,
                frustradas,
                pendOficial,
                evasoes,
                escalonadas,
                Instant.now(),
                hash
        );
    }

    @PjbClusterSingletonTask(key = "citacao-intimacao-presuncoes", ttl = "PT2M")
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void processarPresuncoesAutomaticas() {
        List<ExpedicaoJudicial> pendentes = expedicaoRepository.findByStatusAndPresuncaoEntregaEmLessThanEqual(
                ExpedicaoJudicial.StatusExpedicao.EXPEDIDA,
                Instant.now()
        );
        int atualizadas = 0;
        for (ExpedicaoJudicial expedicao : pendentes) {
            expedicao.marcarPresumidaEntregue();
            expedicaoRepository.save(expedicao);
            Processo processo = processoRepository.findProcessoCompletoById(expedicao.getProcessoId()).orElse(null);
            iniciarPrazoEReveliaSeCabivel(expedicao);
            notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.PRESUMIDA_ENTREGUE);
            propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.PRESUMIDA_ENTREGUE);
            publicarWebhookExpedicao(expedicao, WebhookOutboundService.EventoWebhook.EXPEDICAO_PRESUMIDA_ENTREGUE, java.util.Map.of());
            atualizadas++;
        }
        if (atualizadas > 0) {
            log.info("[CitacaoEngine] Presunção de entrega aplicada em {} expedições.", atualizadas);
            auditLedger.appendSafely(
                    "EXPEDICAO_PRESUNCAO_BATCH",
                    RESOURCE_TYPE,
                    "batch:presuncao",
                    String.valueOf(atualizadas),
                    "Presunção automática de entrega aplicada em lote."
            );
        }
    }

    @PjbClusterSingletonTask(key = "citacao-intimacao-evasoes", ttl = "PT4M")
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void escalonarEvasoesNaoTratadas() {
        List<ExpedicaoJudicial> pendentes = expedicaoRepository.findEvasoesNaoEscalonadas();
        for (ExpedicaoJudicial expedicao : pendentes) {
            try {
                Processo processo = processoRepository.findProcessoCompletoById(expedicao.getProcessoId()).orElse(null);
                if (processo == null) {
                    continue;
                }
                expedicao.escalonarParaJuiz();
                expedicaoRepository.save(expedicao);
                notificarJuizEvasao(expedicao, processo);
                log.info("[CitacaoEngine] Evasão escalonada ao juízo: uuid={}", expedicao.getExpedicaoUuid());
            } catch (Exception e) {
                log.warn("[CitacaoEngine] Falha ao escalonar evasão uuid={}: {}", expedicao.getExpedicaoUuid(), e.getMessage());
            }
        }
    }

    private ModalidadeExpedicaoJudicial resolverModalidadeOtima(ExpedicaoRequest request,
                                                                Processo processo,
                                                                List<String> alertas,
                                                                List<String> cascata) {
        if (request.forcarOficial()) {
            cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [forçado]");
            return ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
        }
        PerfilDestinatario dest = request.destinatario();
        TipoComunicacaoJudicial tipo = request.tipoComunicacao();
        ProceduralCommunicationDecision decisao = matrizResolver.resolver(processo, tipo, dest);
        if (decisao.fundamentoSintetico() != null && !decisao.fundamentoSintetico().isBlank()) {
            alertas.add(decisao.fundamentoSintetico());
        }
        if (!decisao.marcadores().isEmpty()) {
            cascata.add("MATRIZ:" + String.join("|", decisao.marcadores()));
        }
        String documento = CitacaoIntimacaoExpedicaoSupport.extrairDocumento(dest);
        if (decisao.priorizarOficialJustica() && !request.forcarDigital() && !tipo.isEdital()) {
            cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [matriz procedimental]");
            return ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
        }
        if (tipo.isExigePessoalidade() && !tipo.isAdmiteDigital()) {
            cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [pessoalidade obrigatória]");
            return ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
        }
        if (tipo.isEdital()) {
            cascata.add("EDITAL_DOU_DJE [edital]");
            return ModalidadeExpedicaoJudicial.EDITAL_DOU_DJE;
        }
        if (dest instanceof PerfilDestinatario.JuizoDeprecado) {
            cascata.add("CARTA_PRECATORIA_DIGITAL [carta precatória]");
            return ModalidadeExpedicaoJudicial.CARTA_PRECATORIA_DIGITAL;
        }
        boolean jaServadoDigital = documento != null && expedicaoRepository.jaFoiServadoDigitalmente(documento);
        if (jaServadoDigital) {
            alertas.add("Destinatário já recebeu expedição digital anterior — canal digital obrigatório.");
        }
        if (request.forcarDigital()) {
            ModalidadeExpedicaoJudicial modalidadeDigital = resolverPreferenciaDigital(dest, tipo, processo, jaServadoDigital, cascata, alertas);
            if (modalidadeDigital != null) {
                return modalidadeDigital;
            }
            alertas.add("Forçamento digital solicitado, mas não houve canal seguro suficiente. Fallback físico aplicado.");
        }
        if (decisao.priorizarRepresentanteDigital() && dest instanceof PerfilDestinatario.PessoaFisica pf && pf.possuiAdvogado()) {
            cascata.add("DIGITAL_DOMICILIO_ELETRONICO_MNI [matriz recursal/representação]");
            return ModalidadeExpedicaoJudicial.DIGITAL_DOMICILIO_ELETRONICO_MNI;
        }
        return switch (dest) {
            case PerfilDestinatario.AdvogadoOab adv -> resolverAdvogado(adv, tipo, cascata, alertas);
            case PerfilDestinatario.DefensorPublico ignored -> {
                cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [defensor público — pessoal]");
                yield ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
            }
            case PerfilDestinatario.MinisterioPublico ignored -> {
                cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [MP — pessoal]");
                yield ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
            }
            case PerfilDestinatario.FazendaPublica ignored -> {
                cascata.add("DIGITAL_EMAIL_CERTIFICADO_ICP [fazenda pública]");
                yield ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
            }
            case PerfilDestinatario.JuizoDeprecado ignored -> {
                cascata.add("CARTA_PRECATORIA_DIGITAL [juízo deprecado]");
                yield ModalidadeExpedicaoJudicial.CARTA_PRECATORIA_DIGITAL;
            }
            case PerfilDestinatario.PessoaJuridica pj -> resolverPessoaJuridica(pj, tipo, processo, jaServadoDigital, cascata, alertas);
            case PerfilDestinatario.PessoaFisica pf -> resolverPessoaFisica(pf, tipo, processo, jaServadoDigital, cascata, alertas);
            default -> throw new IllegalStateException("Perfil de destinatário não suportado: " + dest);
        };
    }

    private ModalidadeExpedicaoJudicial resolverPreferenciaDigital(PerfilDestinatario dest,
                                                                   TipoComunicacaoJudicial tipo,
                                                                   Processo processo,
                                                                   boolean jaServadoDigital,
                                                                   List<String> cascata,
                                                                   List<String> alertas) {
        if (!tipo.isAdmiteDigital() && !jaServadoDigital) {
            return null;
        }
        return switch (dest) {
            case PerfilDestinatario.AdvogadoOab adv -> resolverAdvogado(adv, tipo, cascata, alertas);
            case PerfilDestinatario.FazendaPublica ignored -> ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
            case PerfilDestinatario.JuizoDeprecado ignored -> ModalidadeExpedicaoJudicial.CARTA_PRECATORIA_DIGITAL;
            case PerfilDestinatario.PessoaJuridica pj -> resolverPessoaJuridica(pj, tipo, processo, true, cascata, alertas);
            case PerfilDestinatario.PessoaFisica pf -> {
                if (pf.possuiContaGovBr()) {
                    cascata.add("DIGITAL_GOVBR_PUSH [forçado digital]");
                    yield ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH;
                }
                if (pf.email() != null && !pf.email().isBlank()) {
                    cascata.add("DIGITAL_EMAIL_CERTIFICADO_ICP [forçado digital]");
                    yield ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
                }
                if (pf.telefone() != null && !pf.telefone().isBlank()) {
                    cascata.add("DIGITAL_SMS_AUTENTICADO [forçado digital]");
                    yield ModalidadeExpedicaoJudicial.DIGITAL_SMS_AUTENTICADO;
                }
                yield null;
            }
            case PerfilDestinatario.DefensorPublico ignored -> null;
            case PerfilDestinatario.MinisterioPublico ignored -> null;
            default -> throw new IllegalStateException("Perfil de destinatário não suportado para preferência digital: " + dest);
        };
    }

    private ModalidadeExpedicaoJudicial resolverAdvogado(PerfilDestinatario.AdvogadoOab adv,
                                                         TipoComunicacaoJudicial tipo,
                                                         List<String> cascata,
                                                         List<String> alertas) {
        if (adv.cadastradoSistemaJudicial()) {
            String sistema = adv.sistemaPrincipal() != null ? adv.sistemaPrincipal() : "PJB";
            cascata.add("DIGITAL_DOMICILIO_ELETRONICO_MNI [advogado cadastrado em " + sistema + "]");
            return ModalidadeExpedicaoJudicial.DIGITAL_DOMICILIO_ELETRONICO_MNI;
        }
        if (adv.emailOab() != null && !adv.emailOab().isBlank()) {
            cascata.add("DIGITAL_EMAIL_CERTIFICADO_ICP [e-mail OAB]");
            alertas.add("Advogado não cadastrado em sistema judicial. Usando e-mail OAB como canal secundário.");
            return ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
        }
        cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [advogado sem cadastro digital]");
        alertas.add("Advogado sem e-mail OAB cadastrado. Mandado físico emitido.");
        return ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
    }

    private ModalidadeExpedicaoJudicial resolverPessoaJuridica(PerfilDestinatario.PessoaJuridica pj,
                                                               TipoComunicacaoJudicial tipo,
                                                               Processo processo,
                                                               boolean jaServadoDigital,
                                                               List<String> cascata,
                                                               List<String> alertas) {
        if (!pj.cnpjAtivo()) {
            alertas.add("CNPJ inativo na Receita Federal. Tentativa via endereço sede registrado.");
            cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [CNPJ inativo]");
            return ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
        }
        Optional<SefazNfeCadastroResolver.CadastroSefazNfe> cadastroSefaz = consultarCadastroSefaz(pj.cnpj(), processo);
        if (!pj.isFazendaPublica() && cadastroSefaz.map(SefazNfeCadastroResolver.CadastroSefazNfe::possuiEmailOperacional).orElse(false)) {
            cascata.add("DIGITAL_SEFAZ_NF_EMAIL [cadastro operacional NF-e/SEFAZ]");
            alertas.add("Cadastro fiscal operacional estadual localizado para o CNPJ. Canal SEFAZ NF-e priorizado.");
            if (jaServadoDigital) {
                alertas.add("Histórico digital anterior confirmado para a pessoa jurídica. Persistindo em rota eletrônica reforçada.");
            }
            return ModalidadeExpedicaoJudicial.DIGITAL_SEFAZ_NF_EMAIL;
        }
        if (pj.possuiPortalGovBr()) {
            cascata.add("PORTAL_EMPRESA_CNPJ [portal Gov.br empresas ativo]");
            return ModalidadeExpedicaoJudicial.PORTAL_EMPRESA_CNPJ;
        }
        if (pj.isGrandeEmpresa() || pj.isBanco() || pj.isFazendaPublica()) {
            alertas.add("Entidade relevante sem portal Gov.br — e-mail Receita Federal priorizado com monitoramento de evasão.");
            cascata.add("DIGITAL_EMAIL_CERTIFICADO_ICP [ente relevante via RF]");
            return ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
        }
        if (pj.emailReceita() != null && !pj.emailReceita().isBlank()) {
            if (jaServadoDigital) {
                cascata.add("DIGITAL_EMAIL_CERTIFICADO_ICP [e-mail RF + histórico digital confirmado]");
                alertas.add("Canal fiscal federal reaproveitado com histórico digital positivo.");
            } else {
                cascata.add("DIGITAL_EMAIL_CERTIFICADO_ICP [e-mail RF — primeira intimação]");
                alertas.add("Pessoa jurídica sem cadastro voluntário. Usando e-mail fiscal federal disponível.");
            }
            return ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
        }
        if (cadastroSefaz.map(SefazNfeCadastroResolver.CadastroSefazNfe::possuiEnderecoFisico).orElse(false)) {
            alertas.add("Cadastro SEFAZ sem e-mail aproveitável, mas com endereço operacional atualizado para fallback físico.");
        } else {
            alertas.add("Pessoa jurídica sem e-mail fiscal útil e sem portal ativo. Endereço cadastrado será usado para Oficial de Justiça.");
        }
        cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [PJ sem dados digitais confiáveis]");
        return ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
    }

    private ModalidadeExpedicaoJudicial resolverPessoaFisica(PerfilDestinatario.PessoaFisica pf,
                                                             TipoComunicacaoJudicial tipo,
                                                             Processo processo,
                                                             boolean jaServadoDigital,
                                                             List<String> cascata,
                                                             List<String> alertas) {
        if (tipo.isExigePessoalidade()) {
            if (pf.possuiContaGovBr() && !tipo.isBloqueiaPresuncaoRebeldia()) {
                cascata.add("DIGITAL_GOVBR_PUSH [pessoal via Gov.br]");
                return ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH;
            }
            cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [pessoalidade + sem Gov.br confiável]");
            return ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
        }
        if (pf.possuiContaGovBr()) {
            cascata.add("DIGITAL_GOVBR_PUSH [conta Gov.br verificada]");
            return ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH;
        }
        if (pf.possuiAdvogado()) {
            cascata.add("DIGITAL_DOMICILIO_ELETRONICO_MNI [via advogado — MNI]");
            return ModalidadeExpedicaoJudicial.DIGITAL_DOMICILIO_ELETRONICO_MNI;
        }
        if (pf.email() != null && !pf.email().isBlank()) {
            cascata.add("DIGITAL_EMAIL_CERTIFICADO_ICP [e-mail cadastrado]");
            return ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
        }
        if (pf.telefone() != null && !pf.telefone().isBlank()) {
            if (tipo.isCitacao()) {
                cascata.add("DIGITAL_WHATSAPP_GOV [telefone com autenticação reforçada]");
                alertas.add("Sem conta Gov.br e sem e-mail. Tentativa via WhatsApp autenticado.");
                return ModalidadeExpedicaoJudicial.DIGITAL_WHATSAPP_GOV;
            }
            cascata.add("DIGITAL_SMS_AUTENTICADO [telefone cadastrado]");
            return ModalidadeExpedicaoJudicial.DIGITAL_SMS_AUTENTICADO;
        }
        cascata.add("OFICIAL_JUSTICA_ROTA_OTIMIZADA [PF sem dados digitais]");
        alertas.add("Pessoa física sem nenhum canal digital disponível. Oficial de Justiça acionado.");
        return ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
    }

    private ModalidadeExpedicaoJudicial escolherFallback(ModalidadeExpedicaoJudicial atual,
                                                         ExpedicaoJudicial expedicao,
                                                         Processo processo) {
        return switch (atual) {
            case DIGITAL_GOVBR_PUSH -> ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
            case DIGITAL_DOMICILIO_ELETRONICO_MNI -> expedicao.getTipoDestinatario() == ExpedicaoJudicial.TipoDestinatario.PESSOA_JURIDICA
                    ? ModalidadeExpedicaoJudicial.DIGITAL_SEFAZ_NF_EMAIL
                    : ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
            case DIGITAL_SEFAZ_NF_EMAIL -> ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
            case DIGITAL_EMAIL_CERTIFICADO_ICP -> ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
            case PORTAL_EMPRESA_CNPJ -> ModalidadeExpedicaoJudicial.DIGITAL_EMAIL_CERTIFICADO_ICP;
            case DIGITAL_WHATSAPP_GOV, DIGITAL_SMS_AUTENTICADO -> ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA;
            case OFICIAL_JUSTICA_ROTA_OTIMIZADA -> expedicao.getTentativasRealizadas() < MAX_TENTATIVAS
                    ? ModalidadeExpedicaoJudicial.CORREIO_AR_DIGITAL
                    : ModalidadeExpedicaoJudicial.EDITAL_DOU_DJE;
            case CORREIO_AR_DIGITAL -> ModalidadeExpedicaoJudicial.EDITAL_DOU_DJE;
            case CARTA_PRECATORIA_DIGITAL -> ModalidadeExpedicaoJudicial.COOPERACAO_JUDICIAL_NACIONAL;
            case COOPERACAO_JUDICIAL_NACIONAL -> ModalidadeExpedicaoJudicial.EDITAL_DOU_DJE;
            case EDITAL_DOU_DJE -> null;
            default -> throw new IllegalStateException("Modalidade de fallback não suportada: " + atual);
        };
    }

    private void despacharModalidade(String expedicaoUuid,
                                     ExpedicaoRequest request,
                                     Long processoId,
                                     ModalidadeExpedicaoJudicial modalidade) {
        try {
            ExpedicaoJudicial expedicao = expedicaoRepository.findByExpedicaoUuid(expedicaoUuid).orElse(null);
            Processo processo = processoRepository.findProcessoCompletoById(processoId).orElse(null);
            if (expedicao == null || processo == null) {
                return;
            }
            switch (modalidade) {
                case DIGITAL_GOVBR_PUSH -> despacharGovBrPush(expedicao, request, processo);
                case DIGITAL_DOMICILIO_ELETRONICO_MNI -> despacharMni(expedicao, request, processo);
                case DIGITAL_SEFAZ_NF_EMAIL -> despacharSefazNfeEmail(expedicao, request, processo);
                case DIGITAL_EMAIL_CERTIFICADO_ICP -> despacharEmailCertificado(expedicao, request, processo);
                case PORTAL_EMPRESA_CNPJ -> despacharPortalCnpj(expedicao, request, processo);
                case DIGITAL_WHATSAPP_GOV -> despacharWhatsAppGov(expedicao, request, processo);
                case DIGITAL_SMS_AUTENTICADO -> despacharSmsAutenticado(expedicao, request, processo);
                case OFICIAL_JUSTICA_ROTA_OTIMIZADA -> despacharOficialJustica(expedicao, request, processo);
                case CORREIO_AR_DIGITAL -> despacharCorreioArDigital(expedicao, request, processo);
                case CARTA_PRECATORIA_DIGITAL -> despacharCartaPrecatoria(expedicao, request, processo);
                case EDITAL_DOU_DJE -> despacharEdital(expedicao, request, processo);
                case COOPERACAO_JUDICIAL_NACIONAL -> despacharCooperacaoJudicial(expedicao, request, processo);
                default -> throw new IllegalStateException("Modalidade de despacho não suportada: " + modalidade);
            }
        } catch (Exception e) {
            log.error("[CitacaoEngine] Falha no despacho uuid={} modalidade={}: {}", expedicaoUuid, modalidade, e.getMessage(), e);
        }
    }

    private void despacharGovBrPush(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setCanalDigitalUtilizado("GOV_BR_PUSH_API_V2");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void despacharMni(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setCanalDigitalUtilizado("MNI_3_0_SOAP");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void despacharSefazNfeEmail(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        enriquecerExpedicaoComCadastroSefaz(expedicao, processo);
        expedicao.setCanalDigitalUtilizado("SEFAZ_NFE_EMAIL_ICP");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void despacharEmailCertificado(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setCanalDigitalUtilizado("EMAIL_ICP_BRASIL_SHA256");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void despacharPortalCnpj(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setCanalDigitalUtilizado("PORTAL_GOV_BR_EMPRESAS_CNPJ");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void despacharWhatsAppGov(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setCanalDigitalUtilizado("WHATSAPP_GOV_BR_AUTENTICADO");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void despacharSmsAutenticado(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setCanalDigitalUtilizado("SMS_TOKEN_UNICO");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void despacharOficialJustica(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setStatus(ExpedicaoJudicial.StatusExpedicao.PENDENTE_OFICIAL);
        expedicaoRepository.save(expedicao);
        gerarQrMandadoSeCabivel(expedicao);
        if (expedicao.getServidorExpedidorId() != null) {
            notificarServidor(expedicao.getServidorExpedidorId(), expedicao, processo, "Mandado físico gerado. Atribuir a Oficial de Justiça.");
        }
        log.info("[CitacaoEngine][OficialJustica] Mandado criado uuid={}", expedicao.getExpedicaoUuid());
    }

    private void despacharCorreioArDigital(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        String codigoRastreio = "PJB" + expedicao.getExpedicaoUuid().replace("-", "").substring(0, 13).toUpperCase(Locale.ROOT);
        expedicao.setCodigoRastreioCorreio(codigoRastreio);
        expedicao.setStatus(ExpedicaoJudicial.StatusExpedicao.REMETIDA_CORREIO);
        expedicaoRepository.save(expedicao);
        gerarQrMandadoSeCabivel(expedicao);
        log.info("[CitacaoEngine][CorreioAR] uuid={} rastreio={}", expedicao.getExpedicaoUuid(), codigoRastreio);
    }

    private void despacharCartaPrecatoria(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setCanalDigitalUtilizado("CARTA_PRECATORIA_PJBR_MESH");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void despacharEdital(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        String numeroEdital = "EDT-" + Instant.now().getEpochSecond() + "-" + expedicao.getProcessoId();
        expedicao.setNumeroEdital(numeroEdital);
        expedicao.setStatus(ExpedicaoJudicial.StatusExpedicao.PUBLICADA_EDITAL);
        expedicaoRepository.save(expedicao);
        CuradorEspecialAutomaticoService curador = curadorProvider.getIfAvailable();
        if (curador != null) {
            curador.registrarNecessidadeSeAusente(expedicao.getProcessoId(), expedicao.getExpedicaoUuid(), CuradorEspecialAutomaticoService.TipoCuradoria.REU_EM_LUGAR_INCERTO);
        }
        notificarPortalDestinatario(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.PUBLICADA_EDITAL);
        propagarAvisoAtendimento(expedicao, processo, ComunicacaoJudicialPortalNotificationService.EventoPortal.PUBLICADA_EDITAL);
        notificarJuizEdital(expedicao, processo, numeroEdital);
        log.warn("[CitacaoEngine][Edital] Último recurso ativado uuid={} edital={}", expedicao.getExpedicaoUuid(), numeroEdital);
    }

    private void despacharCooperacaoJudicial(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        expedicao.setCanalDigitalUtilizado("COOPERACAO_CNJ_350_2020");
        expedicaoRepository.save(expedicao);
        executarInterceptacaoDigital(expedicao, request, processo);
    }

    private void executarInterceptacaoDigital(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        MotorInterceptacaoAtiva motor = motorInterceptacaoProvider.getIfAvailable();
        if (motor == null) {
            log.warn("[CitacaoEngine] MotorInterceptacaoAtiva indisponível para uuid={}", expedicao.getExpedicaoUuid());
            return;
        }
        enriquecerExpedicaoComCadastroSefaz(expedicao, processo);
        List<ViaInterceptacao> vias = CitacaoIntimacaoExpedicaoSupport.montarVias(request.destinatario(), processo, expedicao);
        if (vias.isEmpty()) {
            registrarFrustracaoEAcionarFallback(expedicao.getExpedicaoUuid(), "Nenhuma via segura de interceptação foi construída para o destinatário.");
            return;
        }
        AlvoJuridico alvo = CitacaoIntimacaoExpedicaoSupport.construirAlvo(expedicao, processo, hsmProperties);
        ReciboCitacaoHsm recibo = motor.interceptarComViasSugeridas(
                alvo,
                request.conteudoDoAto().getBytes(StandardCharsets.UTF_8),
                vias
        );
        if (recibo.foiEntregue()) {
            registrarEntregaPorInterceptacao(expedicao.getExpedicaoUuid(), recibo);
            log.info("[CitacaoEngine] Interceptação bem-sucedida uuid={} canal={}", expedicao.getExpedicaoUuid(), recibo.canalVencedor());
        } else {
            motor.acionarFallbackFisicoSeNecessario(recibo, expedicao.getExpedicaoUuid());
        }
    }

    private void notificarJuizEvasao(ExpedicaoJudicial expedicao, Processo processo) {
        if (expedicao.getJuizResponsavelId() == null) {
            return;
        }
        usuarioRepository.findById(expedicao.getJuizResponsavelId()).ifPresent(juiz -> {
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    juiz.getId(),
                    expedicao.getProcessoId(),
                    NotificacaoInteligentePJB.TipoAlerta.ALERTA_REGRA_CRITICA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.CRITICA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }

    private void notificarJuizEdital(ExpedicaoJudicial expedicao, Processo processo, String numeroEdital) {
        if (expedicao.getJuizResponsavelId() == null) {
            return;
        }
        usuarioRepository.findById(expedicao.getJuizResponsavelId()).ifPresent(juiz -> {
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    juiz.getId(),
                    expedicao.getProcessoId(),
                    NotificacaoInteligentePJB.TipoAlerta.MOVIMENTACAO_NOVA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.ALTA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }

    private void notificarServidor(Long servidorId, ExpedicaoJudicial expedicao, Processo processo, String mensagem) {
        usuarioRepository.findById(servidorId).ifPresent(servidor -> {
            NotificacaoInteligentePJB.NotificacaoPJB notif = notificacaoEngine.construir(
                    servidor.getId(),
                    expedicao.getProcessoId(),
                    NotificacaoInteligentePJB.TipoAlerta.MOVIMENTACAO_NOVA,
                    NotificacaoInteligentePJB.UrgenciaMensagem.MEDIA,
                    NotificacaoInteligentePJB.CanalNotificacao.PUSH_APP_PJB
            );
            notificacaoEngine.enviarNotificacao(notif);
        });
    }

    private void notificarPortalDestinatario(ExpedicaoJudicial expedicao,
                                           Processo processo,
                                           ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        ComunicacaoJudicialPortalNotificationService service = portalNotificationProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            service.notificar(expedicao, processo, evento);
        } catch (Exception ex) {
            log.warn("[CitacaoEngine] Falha ao notificar portal destinatário uuid={}: {}", expedicao.getExpedicaoUuid(), ex.getMessage());
        }
    }

    private void propagarAvisoAtendimento(ExpedicaoJudicial expedicao,
                                         Processo processo,
                                         ComunicacaoJudicialPortalNotificationService.EventoPortal evento) {
        ComunicacaoJudicialAtendimentoRelayService service = atendimentoRelayProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        try {
            service.propagarAviso(expedicao, processo, evento);
        } catch (Exception ex) {
            log.warn("[CitacaoEngine] Falha ao propagar aviso de atendimento uuid={}: {}", expedicao.getExpedicaoUuid(), ex.getMessage());
        }
    }

    private void enriquecerExpedicao(ExpedicaoJudicial expedicao, ExpedicaoRequest request, Processo processo) {
        Usuario atual = currentUserService.getOptional().orElse(null);
        expedicao.setJuizResponsavelId(resolverJuizResponsavelId(request.juizResponsavelId(), atual));
        expedicao.setServidorExpedidorId(resolverServidorExpedidorId(request.servidorExpedidorId(), atual));
        if (processo.getJurisdicao() != null) {
            expedicao.setInstanciaExpedidora(processo.getJurisdicao().getCodigo());
        }
        switch (request.destinatario()) {
            case PerfilDestinatario.PessoaFisica pf -> {
                expedicao.setDestinatarioEmail(pf.email());
                expedicao.setDestinatarioTelefone(pf.telefone());
            }
            case PerfilDestinatario.PessoaJuridica pj -> {
                expedicao.setDestinatarioEmail(pj.emailReceita());
                expedicao.setDestinatarioTelefone(pj.telefoneReceita());
                expedicao.setDestinatarioEnderecoEntrega(pj.enderecoSede());
            }
            case PerfilDestinatario.AdvogadoOab adv -> expedicao.setDestinatarioEmail(adv.emailOab());
            case PerfilDestinatario.DefensorPublico defensor -> expedicao.setDestinatarioEmail(defensor.emailInstitucional());
            case PerfilDestinatario.MinisterioPublico mp -> expedicao.setDestinatarioEmail(mp.emailInstitucional());
            case PerfilDestinatario.FazendaPublica fazenda -> expedicao.setDestinatarioEmail(fazenda.emailPgfn());
            case PerfilDestinatario.JuizoDeprecado juizo -> expedicao.setDestinatarioEmail(juizo.emailCarta());
            default -> throw new IllegalStateException("Perfil de destinatário não suportado para enriquecimento: " + request.destinatario());
        }
    }

    private void validarRequest(ExpedicaoRequest request) {
        java.util.Objects.requireNonNull(request, "request");
        java.util.Objects.requireNonNull(request.destinatario(), "request.destinatario");
        java.util.Objects.requireNonNull(request.tipoComunicacao(), "request.tipoComunicacao");
        if (request.processoId() == null) {
            throw new IllegalArgumentException("processoId obrigatório para expedição judicial");
        }
    }

    private String recuperarHashAnterior(String documento, Long processoId) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        return expedicaoRepository.findTop100ByDestinatarioDocumentoOrderByExpedidaEmDesc(documento).stream()
                .filter(item -> processoId == null || java.util.Objects.equals(item.getProcessoId(), processoId))
                .map(ExpedicaoJudicial::getHashIntegridade)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private void publicarWebhookExpedicao(ExpedicaoJudicial expedicao, WebhookOutboundService.EventoWebhook evento, java.util.Map<?, ?> metadados) {
        WebhookOutboundService webhook = webhookProvider.getIfAvailable();
        if (webhook == null || expedicao == null || evento == null) {
            return;
        }
        java.util.LinkedHashMap<String, String> payload = new java.util.LinkedHashMap<>();
        if (metadados != null) {
            metadados.forEach((k, v) -> {
                if (k != null && v != null) payload.put(String.valueOf(k), String.valueOf(v));
            });
        }
        webhook.publicarEventoExpedicao(expedicao, evento, payload);
    }

    private void agendarDespachoAposCommit(String motivo, ExpedicaoRequest request, Long processoId, ModalidadeExpedicaoJudicial modalidade) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            executarDespachoGovernado(motivo, request, processoId, modalidade);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executarDespachoGovernado(motivo, request, processoId, modalidade);
            }
        });
    }

    private void executarDespachoGovernado(String motivo, ExpedicaoRequest request, Long processoId, ModalidadeExpedicaoJudicial modalidade) {
        executionOrchestrator.run(
                PjbExecutionDescriptor.job("citacao-intimacao.despacho-post-commit", Duration.ofSeconds(30)),
                () -> {
                    auditLedger.appendSafely("EXPEDICAO_DESPACHO_POST_COMMIT", RESOURCE_TYPE, String.valueOf(processoId), null, "motivo=" + motivo + " modalidade=" + modalidade);
                    despacharModalidade(motivo, request, processoId, modalidade);
                }
        ).whenComplete((ignored, error) -> {
            if (error != null) {
                log.error("[CitacaoEngine] Falha governada no despacho pós-commit processo={} modalidade={}: {}", processoId, modalidade, error.getMessage(), error);
            }
        });
    }

    private void iniciarPrazoEReveliaSeCabivel(ExpedicaoJudicial expedicao) {
        if (expedicao == null || !expedicao.isEntregueOuLida()) {
            return;
        }
        PrazoRespostaPosEntregaEngine prazoEngine = prazoProvider.getIfAvailable();
        if (prazoEngine == null) {
            return;
        }
        PrazoRespostaPosEntregaEngine.PrazoResposta prazo = prazoEngine.iniciarPrazoAposEntrega(
                expedicao.getExpedicaoUuid(),
                CitacaoIntimacaoExpedicaoSupport.resolverTipoUsuarioPrazo(expedicao),
                CitacaoIntimacaoExpedicaoSupport.resolverTipoPrazoPadrao(expedicao)
        );
        ReveliaAutomaticaEngine reveliaEngine = reveliaProvider.getIfAvailable();
        if (reveliaEngine != null && prazo != null && prazo.vencimentoEm() != null) {
            reveliaEngine.iniciarMonitoramento(expedicao.getExpedicaoUuid(), expedicao.getProcessoId(), prazo.vencimentoEm());
        }
    }

    private java.util.Optional<SefazNfeCadastroResolver.CadastroSefazNfe> consultarCadastroSefaz(String cnpj, Processo processo) {
        SefazNfeCadastroResolver resolver = sefazCadastroResolverProvider.getIfAvailable();
        if (resolver == null || processo == null) {
            return java.util.Optional.empty();
        }
        String uf = processo.getUf();
        if ((uf == null || uf.isBlank()) && processo.getJurisdicao() != null) {
            uf = processo.getJurisdicao().getEstado();
        }
        return resolver.resolver(cnpj, uf, processo.getTribunalCodigoRoteado());
    }

    private void enriquecerExpedicaoComCadastroSefaz(ExpedicaoJudicial expedicao, Processo processo) {
        if (expedicao == null || processo == null || expedicao.getDestinatarioDocumento() == null) {
            return;
        }
        consultarCadastroSefaz(expedicao.getDestinatarioDocumento(), processo).ifPresent(cadastro -> {
            if ((expedicao.getDestinatarioEmail() == null || expedicao.getDestinatarioEmail().isBlank()) && cadastro.possuiEmailOperacional()) {
                expedicao.setDestinatarioEmail(cadastro.emailOperacional());
            }
            if ((expedicao.getDestinatarioTelefone() == null || expedicao.getDestinatarioTelefone().isBlank()) && cadastro.telefoneOperacional() != null && !cadastro.telefoneOperacional().isBlank()) {
                expedicao.setDestinatarioTelefone(cadastro.telefoneOperacional());
            }
            if ((expedicao.getDestinatarioEnderecoEntrega() == null || expedicao.getDestinatarioEnderecoEntrega().isBlank()) && cadastro.possuiEnderecoFisico()) {
                expedicao.setDestinatarioEnderecoEntrega(cadastro.enderecoEstabelecimento());
            }
        });
    }

    private void gerarQrMandadoSeCabivel(ExpedicaoJudicial expedicao) {
        if (expedicao == null || expedicao.getModalidade() != ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA) {
            return;
        }
        QrCodeMandadoService service = qrCodeProvider.getIfAvailable();
        if (service != null) {
            service.gerar(expedicao.getExpedicaoUuid());
        }
    }

    private Long resolverJuizResponsavelId(Long juizResponsavelId, Usuario atual) {
        if (juizResponsavelId != null) return juizResponsavelId;
        return atual == null ? null : atual.getId();
    }

    private Long resolverServidorExpedidorId(Long servidorExpedidorId, Usuario atual) {
        if (servidorExpedidorId != null) return servidorExpedidorId;
        return atual == null ? null : atual.getId();
    }

}
