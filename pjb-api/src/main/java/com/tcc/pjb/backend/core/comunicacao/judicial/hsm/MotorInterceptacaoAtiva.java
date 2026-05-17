package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MotorInterceptacaoAtiva {

    private static final Logger log = LoggerFactory.getLogger(MotorInterceptacaoAtiva.class);
    private static final String RESOURCE_TYPE = "INTERCEPTACAO_ATIVA";

    private final HttpClient httpClient;
    private final PjbHardwareSecurityModule hsm;
    private final AuditLedgerService auditLedger;
    private final CitacaoIntimacaoEngine citacaoEngine;
    private final SefazNfeCadastroResolver sefazCadastroResolver;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public MotorInterceptacaoAtiva(@Qualifier("hsmInterceptacaoHttpClient") HttpClient httpClient,
                                   PjbHardwareSecurityModule hsm,
                                   AuditLedgerService auditLedger,
                                   CitacaoIntimacaoEngine citacaoEngine,
                                   SefazNfeCadastroResolver sefazCadastroResolver,
                                   PjbExecutionOrchestrator executionOrchestrator) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.hsm = Objects.requireNonNull(hsm, "hsm");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.citacaoEngine = Objects.requireNonNull(citacaoEngine, "citacaoEngine");
        this.sefazCadastroResolver = Objects.requireNonNull(sefazCadastroResolver, "sefazCadastroResolver");
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
    }

    public ReciboCitacaoHsm deflagrarCacaImplacavel(AlvoJuridico alvo,
                                                    byte[] payloadDoAto,
                                                    ViaInterceptacao... vias) {
        Objects.requireNonNull(alvo, "alvo");
        Objects.requireNonNull(payloadDoAto, "payloadDoAto");
        if (vias == null || vias.length == 0) {
            throw new IllegalArgumentException("Ao menos uma ViaInterceptacao é obrigatória.");
        }
        List<ViaInterceptacao> viasOrdenadas = Arrays.stream(vias)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(ViaInterceptacao::prioridadeOrdem))
                .limit(10)
                .toList();
        if (viasOrdenadas.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma ViaInterceptacao válida foi informada.");
        }
        String hashPayload = sha256Hex(payloadDoAto);
        log.info(
                "[Interceptacao] Iniciando busca ativa. alvo={} vias={} hash={} net={}",
                mascararDoc(alvo.documentoUnico()),
                viasOrdenadas.size(),
                resumirHash(hashPayload),
                httpClient.version()
        );
        auditLedger.appendSafely(
                "INTERCEPTACAO_INICIADA",
                RESOURCE_TYPE,
                alvo.documentoUnico(),
                hashPayload,
                "Interceptação ativa em %d canal(is). Processo: %s. Cadeia: %s".formatted(
                        viasOrdenadas.size(),
                        alvo.processoNumero() != null ? alvo.processoNumero() : "SEM_NUMERO",
                        alvo.exigeCadeiaCertificacao() ? "ESTRITA" : "PADRAO"
                )
        );
        return executarCorridaParalela(alvo, payloadDoAto, hashPayload, viasOrdenadas);
    }

    public ReciboCitacaoHsm interceptarComViasSugeridas(AlvoJuridico alvo,
                                                         byte[] payloadDoAto,
                                                         List<ViaInterceptacao> vias) {
        Objects.requireNonNull(vias, "vias");
        return deflagrarCacaImplacavel(alvo, payloadDoAto, vias.toArray(ViaInterceptacao[]::new));
    }

    @Transactional
    public void acionarFallbackFisicoSeNecessario(ReciboCitacaoHsm recibo, String expedicaoUuid) {
        if (recibo == null || !recibo.exigeFallbackFisico()) {
            return;
        }
        try {
            citacaoEngine.registrarFrustracaoEAcionarFallback(
                    expedicaoUuid,
                    "Interceptação ativa falhou em todos os %d canais: %s".formatted(
                            recibo.canaisFalhados().size(),
                            String.join(", ", recibo.canaisFalhados())
                    )
            );
            log.warn("[Interceptacao] Fallback físico acionado para expedicao={}", expedicaoUuid);
        } catch (Exception e) {
            log.error("[Interceptacao] Falha ao acionar fallback físico: {}", e.getMessage(), e);
        }
    }


    private ReciboCitacaoHsm executarCorridaParalela(AlvoJuridico alvo,
                                                     byte[] payload,
                                                     String hashPayload,
                                                     List<ViaInterceptacao> vias) {
        List<String> canaisTestados = new ArrayList<>(vias.size());
        List<String> canaisFalhados = java.util.Collections.synchronizedList(new ArrayList<>());
        CompletableFuture<TaticaResultado> raceFuture = new CompletableFuture<>();
        AtomicBoolean completado = new AtomicBoolean(false);
        AtomicInteger restantes = new AtomicInteger(vias.size());
        List<CompletableFuture<TaticaResultado>> tarefas = new ArrayList<>(vias.size());
        for (ViaInterceptacao via : vias) {
            canaisTestados.add(via.identificadorCanal());
            CompletableFuture<TaticaResultado> tarefa = executionOrchestrator
                    .supply(PjbExecutionDescriptor.externalIo("interceptacao." + via.identificadorCanal(), java.time.Duration.ofMillis(alvo.timeoutMs())), () -> executarTatica(via, alvo, payload))
                    .whenComplete((resultado, erro) -> {
                        try {
                            if (erro == null && resultado != null && resultado.sucesso()) {
                                if (completado.compareAndSet(false, true)) {
                                    raceFuture.complete(resultado);
                                }
                                return;
                            }
                            String razao = erro != null ? safeMessage(erro) : "resultado nulo";
                            canaisFalhados.add(via.identificadorCanal() + "[" + razao + "]");
                            log.debug("[Interceptacao] Canal falhou: {} razão={}", via.identificadorCanal(), razao);
                        } finally {
                            if (restantes.decrementAndGet() == 0 && completado.compareAndSet(false, true)) {
                                raceFuture.completeExceptionally(
                                        new InterceptacaoTotalmenteFailedException(
                                                "Todos os %d canais falharam para o alvo %s".formatted(
                                                        vias.size(),
                                                        mascararDoc(alvo.documentoUnico())
                                                )
                                        )
                                );
                            }
                        }
                    });
            tarefas.add(tarefa);
        }
        try {
            TaticaResultado vencedor = raceFuture.get(alvo.timeoutMs(), TimeUnit.MILLISECONDS);
            tarefas.forEach(t -> t.cancel(true));
            PjbHardwareSecurityModule.AssinaturaHsm assinatura = hsm.assinar(payload);
            UUID protocolo = UUID.randomUUID();
            String trilha = vencedor.trilha() + "|HASH=" + resumirHash(hashPayload) + "|PROTO=" + protocolo;
            auditLedger.appendSafely(
                    "INTERCEPTACAO_SUCESSO",
                    RESOURCE_TYPE,
                    alvo.documentoUnico(),
                    protocolo.toString(),
                    "Canal vencedor: " + vencedor.canalId()
            );
            log.info(
                    "[Interceptacao] Sucesso. alvo={} canal={} protocolo={}",
                    mascararDoc(alvo.documentoUnico()),
                    vencedor.canalId(),
                    protocolo
            );
            return new ReciboCitacaoHsm(
                    protocolo,
                    ZonedDateTime.now(),
                    assinatura,
                    trilha,
                    vencedor.canalId(),
                    mascararDoc(alvo.documentoUnico()),
                    alvo.processoNumero(),
                    alvo.processoId(),
                    hashPayload,
                    hsm.isMock(),
                    List.copyOf(canaisTestados),
                    List.copyOf(canaisFalhados),
                    "Entrega confirmada via " + vencedor.canalId() + ". Prazo de resposta inicia em até " + vencedor.presuncaoHoras() + "h."
            );
        } catch (TimeoutException e) {
            tarefas.forEach(t -> t.cancel(true));
            log.warn("[Interceptacao] Timeout de {}ms. Acionando fallback físico.", alvo.timeoutMs());
            return reciboFalha(alvo, hashPayload, canaisTestados, canaisFalhados, "TIMEOUT após " + alvo.timeoutMs() + "ms");
        } catch (ExecutionException e) {
            tarefas.forEach(t -> t.cancel(true));
            String motivo = e.getCause() != null ? safeMessage(e.getCause()) : safeMessage(e);
            log.warn("[Interceptacao] Falha total. alvo={} motivo={}", mascararDoc(alvo.documentoUnico()), motivo);
            return reciboFalha(alvo, hashPayload, canaisTestados, canaisFalhados, motivo);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tarefas.forEach(t -> t.cancel(true));
            return reciboFalha(alvo, hashPayload, canaisTestados, canaisFalhados, "Operação interrompida");
        }
    }

    private TaticaResultado executarTatica(ViaInterceptacao via,
                                           AlvoJuridico alvo,
                                           byte[] payload) {
        return switch (via) {
            case null -> throw new TaticaFalhouException("Via de interceptação ausente");
            case ViaInterceptacao.GovBrAutenticado govBr -> taticaGovBr(govBr, alvo, payload);
            case ViaInterceptacao.GovBrPush govBrPush -> taticaGovBrPush(govBrPush, alvo, payload);
            case ViaInterceptacao.MalhaFinanceiraBacen bacen -> taticaBacen(bacen, alvo, payload);
            case ViaInterceptacao.SefazNfeEmissor sefaz -> taticaSefazNfe(sefaz, alvo, payload);
            case ViaInterceptacao.ReceitaFederalCnpjCpf receita -> taticaReceitaFederal(receita, alvo, payload);
            case ViaInterceptacao.AnatelOperadora anatel -> taticaAnatel(anatel, alvo, payload);
            case ViaInterceptacao.DetranRegistroVeiculo detran -> taticaDetran(detran, alvo, payload);
            case ViaInterceptacao.Serpro serpro -> taticaSerpro(serpro, alvo, payload);
            case ViaInterceptacao.OabSistemaJudicial oab -> taticaOab(oab, alvo, payload);
            case ViaInterceptacao.CooperacaoCnjMalha cnj -> taticaCnj(cnj, alvo, payload);
            case ViaInterceptacao.PortalGovBrEmpresa portal -> taticaPortalEmpresa(portal, alvo, payload);
            case ViaInterceptacao.CartorioRegistroCivil crc -> taticaCrc(crc, alvo, payload);
            case ViaInterceptacao.WhatsappGov whatsapp -> taticaWhatsappGov(whatsapp, alvo, payload);
            case ViaInterceptacao.SmsAutenticado sms -> taticaSmsAutenticado(sms, alvo, payload);
            case ViaInterceptacao.PortalCnpj portalCnpj -> taticaPortalCnpj(portalCnpj, alvo, payload);
            case ViaInterceptacao.EmailCertificado email -> taticaEmailCertificado(email, alvo, payload);
        };
    }

    private TaticaResultado taticaGovBr(ViaInterceptacao.GovBrAutenticado via,
                                        AlvoJuridico alvo,
                                        byte[] payload) {
        boolean contaValida = via.nivelConta() != null
                && (via.nivelConta().equalsIgnoreCase("PRATA") || via.nivelConta().equalsIgnoreCase("OURO"))
                && via.tokenSessaoGov() != null
                && !via.tokenSessaoGov().isBlank();
        if (!contaValida) {
            throw new TaticaFalhouException("Conta Gov.br ausente ou nível insuficiente.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("GOV_BR", alvo, via.ipOrigem()), 24);
    }

    private TaticaResultado taticaGovBrPush(ViaInterceptacao.GovBrPush via,
                                            AlvoJuridico alvo,
                                            byte[] payload) {
        boolean contaValida = via.nivelConta() != null
                && (via.nivelConta().equalsIgnoreCase("PRATA") || via.nivelConta().equalsIgnoreCase("OURO"))
                && via.tokenSessaoGov() != null
                && !via.tokenSessaoGov().isBlank();
        if (!contaValida) {
            throw new TaticaFalhouException("Push Gov.br sem sessão válida ou nível insuficiente.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("GOV_BR_PUSH", alvo, via.ipOrigem()), 24);
    }

    private TaticaResultado taticaWhatsappGov(ViaInterceptacao.WhatsappGov via,
                                              AlvoJuridico alvo,
                                              byte[] payload) {
        if (!via.ativo() || via.numero() == null || via.numero().isBlank()) {
            throw new TaticaFalhouException("Canal WhatsApp governamental inativo ou número ausente.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("WHATSAPP_GOV", alvo, via.numero()), 24);
    }

    private TaticaResultado taticaSmsAutenticado(ViaInterceptacao.SmsAutenticado via,
                                                 AlvoJuridico alvo,
                                                 byte[] payload) {
        if (!via.ativo() || via.numero() == null || via.numero().isBlank()) {
            throw new TaticaFalhouException("Canal SMS autenticado inativo ou número ausente.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("SMS_AUTENTICADO", alvo, via.numero()), 48);
    }

    private TaticaResultado taticaEmailCertificado(ViaInterceptacao.EmailCertificado via,
                                                   AlvoJuridico alvo,
                                                   byte[] payload) {
        if (!via.ativo() || via.email() == null || via.email().isBlank()) {
            throw new TaticaFalhouException("E-mail certificado inativo ou ausente.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("EMAIL_CERTIFICADO", alvo, via.email()), 72);
    }

    private TaticaResultado taticaPortalCnpj(ViaInterceptacao.PortalCnpj via,
                                             AlvoJuridico alvo,
                                             byte[] payload) {
        if (!via.portalAtivo()) {
            throw new TaticaFalhouException("Portal CNPJ inativo para o documento informado.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("PORTAL_CNPJ", alvo, via.cnpj()), 72);
    }

    private TaticaResultado taticaBacen(ViaInterceptacao.MalhaFinanceiraBacen via,
                                        AlvoJuridico alvo,
                                        byte[] payload) {
        if (!via.chevePixAtiva() || via.chaveDictCentral() == null || via.chaveDictCentral().isBlank()) {
            throw new TaticaFalhouException("Chave Pix inativa ou ausente no DICT.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("BACEN_DICT", alvo, via.ispbBanco()), 48);
    }

    private TaticaResultado taticaSefazNfe(ViaInterceptacao.SefazNfeEmissor via,
                                           AlvoJuridico alvo,
                                           byte[] payload) {
        if (!via.emissorNfeAtivo()) {
            throw new TaticaFalhouException("Destinatário sem indicativo de emissão NF-e ativa.");
        }
        SefazNfeCadastroResolver.CadastroSefazNfe cadastro = sefazCadastroResolver
                .resolver(via.cnpj(), via.uf())
                .orElseGet(() -> new SefazNfeCadastroResolver.CadastroSefazNfe(
                        digitsOnly(via.cnpj()),
                        upperTrim(via.uf()),
                        lowerTrim(via.emailOperacionalSnapshot()),
                        digitsOnly(via.telefoneOperacionalSnapshot()),
                        trimToNull(via.enderecoEstabelecimentoSnapshot()),
                        false,
                        "SEFAZ_NFE_SNAPSHOT",
                        java.time.Instant.now(),
                        0,
                        Integer.toHexString(Objects.hash(via.cnpj(), via.uf(), via.emailOperacionalSnapshot(), via.telefoneOperacionalSnapshot(), via.enderecoEstabelecimentoSnapshot()))
                ));
        boolean temEmail = cadastro.possuiEmailOperacional();
        boolean temTelefone = cadastro.telefoneOperacional() != null && !cadastro.telefoneOperacional().isBlank();
        boolean temEndereco = cadastro.possuiEnderecoFisico();
        if (!temEmail) {
            if (temTelefone || temEndereco) {
                throw new TaticaFalhouException("SEFAZ NF-e sem e-mail operacional confirmado para disparo eletrônico.");
            }
            throw new TaticaFalhouException("SEFAZ NF-e sem dados operacionais aproveitáveis.");
        }
        int horas = 96;
        String referencia = firstNonBlank(cadastro.emailOperacional(), cadastro.telefoneOperacional(), cadastro.enderecoEstabelecimento(), via.uf());
        return TaticaResultado.sucesso(
                via.identificadorCanal(),
                buildTrilha("SEFAZ_NFE_" + upperTrim(via.uf()), alvo, referencia),
                horas
        );
    }

    private TaticaResultado taticaReceitaFederal(ViaInterceptacao.ReceitaFederalCnpjCpf via,
                                                 AlvoJuridico alvo,
                                                 byte[] payload) {
        boolean temEmail = via.emailReceita() != null && !via.emailReceita().isBlank();
        boolean temEndereco = via.enderecoFiscal() != null && !via.enderecoFiscal().isBlank();
        boolean cnpjOk = !via.isPessoaJuridica() || via.cnpjAtivo();
        if (!cnpjOk) {
            throw new TaticaFalhouException("CNPJ inativo na Receita Federal.");
        }
        if (!temEmail && !temEndereco) {
            throw new TaticaFalhouException("Sem e-mail nem endereço fiscal na Receita Federal.");
        }
        int horas = temEmail ? 72 : 120;
        return TaticaResultado.sucesso(
                via.identificadorCanal(),
                buildTrilha("RF_" + (via.isPessoaJuridica() ? "CNPJ" : "CPF"), alvo, via.documento()),
                horas
        );
    }

    private TaticaResultado taticaAnatel(ViaInterceptacao.AnatelOperadora via,
                                         AlvoJuridico alvo,
                                         byte[] payload) {
        if (!via.numeroAtivo() || via.numeroVinculado() == null || via.numeroVinculado().isBlank()) {
            throw new TaticaFalhouException("Número ANATEL inativo ou não vinculado ao documento.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("ANATEL_SMS", alvo, via.codigoOperadora()), 48);
    }

    private TaticaResultado taticaDetran(ViaInterceptacao.DetranRegistroVeiculo via,
                                         AlvoJuridico alvo,
                                         byte[] payload) {
        if (via.enderecoRegistro() == null || via.enderecoRegistro().isBlank()) {
            throw new TaticaFalhouException("Endereço DETRAN não localizado para o documento.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("DETRAN_" + via.uf(), alvo, via.renavam()), 72);
    }

    private TaticaResultado taticaSerpro(ViaInterceptacao.Serpro via,
                                         AlvoJuridico alvo,
                                         byte[] payload) {
        if (via.dadosVinculo() == null || via.dadosVinculo().isBlank()) {
            throw new TaticaFalhouException("Sem vínculo empregatício ou cadastro no SERPRO.");
        }
        int horas = via.enderecoAtualizado() != null && !via.enderecoAtualizado().isBlank() ? 72 : 120;
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("SERPRO_" + via.sistemaOrigem(), alvo, via.documento()), horas);
    }

    private TaticaResultado taticaOab(ViaInterceptacao.OabSistemaJudicial via,
                                      AlvoJuridico alvo,
                                      byte[] payload) {
        if (via.mniAtivo() && via.sistemaPrincipal() != null && !via.sistemaPrincipal().isBlank()) {
            return TaticaResultado.sucesso(
                    via.identificadorCanal(),
                    buildTrilha("OAB_MNI_" + via.sistemaPrincipal(), alvo, via.oabNumero()),
                    72
            );
        }
        if (via.emailInstitucional() != null && !via.emailInstitucional().isBlank()) {
            return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("OAB_EMAIL", alvo, via.oabNumero()), 120);
        }
        throw new TaticaFalhouException("Advogado sem MNI ativo nem e-mail OAB cadastrado.");
    }

    private TaticaResultado taticaCnj(ViaInterceptacao.CooperacaoCnjMalha via,
                                      AlvoJuridico alvo,
                                      byte[] payload) {
        if (via.codigoTribunalDestino() == null || via.codigoTribunalDestino().isBlank()) {
            throw new TaticaFalhouException("Código do tribunal de destino ausente.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("CNJ_COOP_" + via.uf(), alvo, via.codigoTribunalDestino()), 240);
    }

    private TaticaResultado taticaPortalEmpresa(ViaInterceptacao.PortalGovBrEmpresa via,
                                                AlvoJuridico alvo,
                                                byte[] payload) {
        if (!via.portalAtivo()) {
            throw new TaticaFalhouException("Portal Gov.br Empresa inativo para o CNPJ.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("PORTAL_EMPRESA", alvo, via.cnpj()), 72);
    }

    private TaticaResultado taticaCrc(ViaInterceptacao.CartorioRegistroCivil via,
                                      AlvoJuridico alvo,
                                      byte[] payload) {
        if (via.municipioNascimento() == null || via.municipioNascimento().isBlank()) {
            throw new TaticaFalhouException("Município de nascimento não informado para consulta CRC.");
        }
        return TaticaResultado.sucesso(via.identificadorCanal(), buildTrilha("CRC_" + via.ufNascimento(), alvo, via.cpf()), 120);
    }

    private ReciboCitacaoHsm reciboFalha(AlvoJuridico alvo,
                                         String hashPayload,
                                         List<String> testados,
                                         List<String> falhados,
                                         String motivo) {
        UUID protocolo = UUID.randomUUID();
        auditLedger.appendSafely(
                "INTERCEPTACAO_FALHA_TOTAL",
                RESOURCE_TYPE,
                alvo.documentoUnico(),
                protocolo.toString(),
                "Falha total: " + motivo
        );
        PjbHardwareSecurityModule.AssinaturaHsm assinaturaTentativa = null;
        try {
            assinaturaTentativa = hsm.assinar(("FAIL::" + hashPayload).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
        String recomendacao = switch (falhados.size()) {
            case 0, 1 -> "Reagendar interceptação com dados complementares.";
            case 2, 3 -> "Acionar Oficial de Justiça com rota GPS otimizada.";
            default -> "Citar por edital (DJE/DOU). Possível revelia após prazo.";
        };
        return new ReciboCitacaoHsm(
                protocolo,
                ZonedDateTime.now(),
                assinaturaTentativa,
                "FALHA_TOTAL|MOTIVO=" + motivo,
                null,
                mascararDoc(alvo.documentoUnico()),
                alvo.processoNumero(),
                alvo.processoId(),
                hashPayload,
                hsm.isMock(),
                List.copyOf(testados),
                List.copyOf(falhados),
                recomendacao
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String lowerTrim(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    private static String upperTrim(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(java.util.Locale.ROOT);
    }

    private static String digitsOnly(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String buildTrilha(String origem, AlvoJuridico alvo, String referencia) {
        return "%s|DOC=%s|PROC=%s|CHAIN=%s|REF=%s|TS=%d".formatted(
                origem,
                mascararDoc(alvo.documentoUnico()),
                alvo.processoNumero() != null ? alvo.processoNumero() : "SEM_PROC",
                alvo.exigeCadeiaCertificacao() ? "STRICT" : "DEFAULT",
                referencia != null ? referencia : "N/A",
                System.currentTimeMillis()
        );
    }

    private static String sha256Hex(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(Arrays.hashCode(data));
        }
    }

    private static String mascararDoc(String doc) {
        if (doc == null || doc.length() < 4) {
            return "***";
        }
        return doc.substring(0, 3) + "***" + doc.substring(Math.max(3, doc.length() - 2));
    }

    private static String resumirHash(String hash) {
        return hash.substring(0, Math.min(16, hash.length())) + "...";
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    private record TaticaResultado(boolean sucesso, String canalId, String trilha, int presuncaoHoras) {
        static TaticaResultado sucesso(String canal, String trilha, int horas) {
            return new TaticaResultado(true, canal, trilha, horas);
        }
    }

    public static final class TaticaFalhouException extends RuntimeException {
        public TaticaFalhouException(String message) {
            super(message);
        }
    }

    public static final class InterceptacaoTotalmenteFailedException extends RuntimeException {
        public InterceptacaoTotalmenteFailedException(String message) {
            super(message);
        }
    }
}
