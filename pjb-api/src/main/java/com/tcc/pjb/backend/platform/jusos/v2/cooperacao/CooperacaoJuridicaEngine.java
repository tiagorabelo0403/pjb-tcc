package com.tcc.pjb.backend.platform.jusos.v2.cooperacao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.PrazoCalculado;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.TipoPrazo;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
@Slf4j
public class CooperacaoJuridicaEngine {
    private static final String RESOURCE_TYPE = "COOPERACAO_JURIDICA";
    private static final String DEFAULT_DESTINO = "DESTINO_NAO_INFORMADO";

    public enum TipoCooperacao {
        CARTA_PRECATORIA_CIVEL,
        CARTA_PRECATORIA_PENAL,
        CARTA_PRECATORIA_TRABALHISTA,
        CARTA_PRECATORIA_CUMPRIMENTO_SENTENCA,
        CARTA_ROGATORIA_ATIVA,
        CARTA_ROGATORIA_PASSIVA,
        AUXILIO_DIRETO_NACIONAL,
        AUXILIO_DIRETO_INTERNACIONAL,
        MLAT_MUTUA_ASSISTENCIA_LEGAL,
        PEDIDO_EXTRADICAO,
        TRANSFERENCIA_PRESO,
        COOPERACAO_ADMINISTRATIVA_CNJ,
        DEPRECADA_PJE_MNI
    }

    public enum StatusCooperacao {
        EXPEDIDA,
        RECEBIDA,
        EM_CUMPRIMENTO,
        CUMPRIDA,
        DEVOLVIDA_SEM_CUMPRIMENTO,
        CANCELADA,
        SUSPENSA,
        AGUARDANDO_APOSTILAMENTO
    }

    public enum CanaisCooperacaoInternacional {
        AUTORIDADE_CENTRAL_MJ("Ministério da Justiça — Autoridade Central no Brasil"),
        MINISTERIO_RELACOES_EXTERIORES("MRE — protocolo diplomático e comunicação consular"),
        INTERPOL_BRASIL("INTERPOL Brasil — DPF — apoio policial internacional"),
        EUROJUST("EUROJUST — cooperação com países da União Europeia"),
        OEA_CONVENCAO("OEA — Convenção Interamericana sobre Cartas Rogatórias"),
        REDE_IBEROAMERICANA_COOPERACAO("RIJA — Rede Iberoamericana de Cooperação Judiciária"),
        MERCOSUL_LAS_LENAS("Protocolo de Las Leñas — cooperação intra-MERCOSUL"),
        CONVENCAO_HAIA("Convenções da Haia — citação, prova e apostilamento");

        public final String descricao;

        CanaisCooperacaoInternacional(String descricao) {
            this.descricao = descricao;
        }
    }

    public record CartaPrecatoria(
            UUID cartaId,
            String numeroProcessoOrigem,
            String juizoDeprecante,
            String tribunalDeprecante,
            String ufDeprecante,
            String juizoDeprecado,
            String tribunalDeprecado,
            String ufDeprecada,
            TipoCooperacao tipo,
            StatusCooperacao status,
            String objetoDiligencia,
            String dadosCumprimento,
            List<String> documentosAnexos,
            Instant expedidaEm,
            Instant prazoMaximoCumprimento,
            Instant cumpridaEm,
            String observacoes
    ) {
        public CartaPrecatoria {
            documentosAnexos = immutableDistinct(documentosAnexos);
        }
    }

    public record CartaRogatoria(
            UUID rogatoriaId,
            String processoOrigem,
            String paisDestino,
            String autoridadeDestinataria,
            TipoCooperacao tipo,
            StatusCooperacao status,
            String objetoPedido,
            CanaisCooperacaoInternacional canal,
            String tratadoBase,
            boolean exigeApostilamento,
            boolean exigeTraducaoJurada,
            List<String> documentosRequeridos,
            Instant expedidaEm,
            String numeroCanalDireto
    ) {
        public CartaRogatoria {
            documentosRequeridos = immutableDistinct(documentosRequeridos);
        }
    }

    public record PlanoCooperacao(
            TipoCooperacao tipoRecomendado,
            List<String> etapas,
            List<String> documentosNecessarios,
            List<String> alertas,
            String fundamentoLegal,
            int prazoEstimadoDias,
            boolean exigeHomologacaoSTJ
    ) {
        public PlanoCooperacao {
            etapas = immutableDistinct(etapas);
            documentosNecessarios = immutableDistinct(documentosNecessarios);
            alertas = immutableDistinct(alertas);
        }
    }

    public record TriagemCooperacao(
            boolean internacional,
            boolean mercosul,
            boolean altaUrgencia,
            boolean altaSensibilidade,
            TipoCooperacao tipoPreferencial,
            CanaisCooperacaoInternacional canalPreferencial,
            boolean recomendaVideoconferencia,
            boolean exigeTraducao,
            boolean exigeApostilamento,
            boolean exigeHomologacaoSTJ,
            String racional
    ) {}

    public record MatrizConformidade(
            double score,
            List<String> pendencias,
            List<String> travas,
            List<String> salvaguardas,
            boolean aptaExpedicao,
            String fingerprintIntegridade
    ) {
        public MatrizConformidade {
            pendencias = immutableDistinct(pendencias);
            travas = immutableDistinct(travas);
            salvaguardas = immutableDistinct(salvaguardas);
        }
    }

    public record ResultadoExpedicao(
            UUID expedienteId,
            Long processoId,
            TipoCooperacao tipo,
            StatusCooperacao status,
            String destinoPrincipal,
            String numeroRastreio,
            boolean internacional,
            boolean exigeTraducaoJurada,
            boolean exigeApostilamento,
            boolean exigeHomologacaoSTJ,
            List<String> pendencias,
            String hashIntegridade,
            Instant registradaEm
    ) {
        public ResultadoExpedicao {
            pendencias = immutableDistinct(pendencias);
        }
    }

    public record PainelCooperacao(
            String numeroUnificado,
            TipoCooperacao tipoSugerido,
            String destinoPrincipal,
            boolean internacional,
            String nivelOperacional,
            List<String> indicadores,
            List<String> proximasAcoes,
            List<String> alertas,
            Instant geradoEm
    ) {
        public PainelCooperacao {
            indicadores = immutableDistinct(indicadores);
            proximasAcoes = immutableDistinct(proximasAcoes);
            alertas = immutableDistinct(alertas);
        }
    }

    public record CooperacaoRegistradaEvent(
            Long processoId,
            String numeroUnificado,
            TipoCooperacao tipo,
            String destinoPrincipal,
            String numeroRastreio,
            Instant registradaEm
    ) {}

    private final ProcessoRepository processoRepository;
    private final AuditLedgerService auditLedger;
    private final CurrentUserService currentUserService;
    private final UiHistoryService uiHistoryService;
    private final NationalRulePackEngine rulePackEngine;
    private final NationalPrazoEngine prazoEngine;
    private final ApplicationEventPublisher eventPublisher;

    public CooperacaoJuridicaEngine(ProcessoRepository processoRepository,
                                    AuditLedgerService auditLedger,
                                    CurrentUserService currentUserService,
                                    UiHistoryService uiHistoryService,
                                    NationalRulePackEngine rulePackEngine,
                                    NationalPrazoEngine prazoEngine,
                                    ApplicationEventPublisher eventPublisher) {
        this.processoRepository = processoRepository;
        this.auditLedger = auditLedger;
        this.currentUserService = currentUserService;
        this.uiHistoryService = uiHistoryService;
        this.rulePackEngine = rulePackEngine;
        this.prazoEngine = prazoEngine;
        this.eventPublisher = eventPublisher;
    }

    public PlanoCooperacao analisarNecessidade(Long processoId, String ufDestino, String paisDestino) {
        Objects.requireNonNull(processoId, "processoId");
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        return analisarNecessidade(processo, ufDestino, paisDestino);
    }

    public PlanoCooperacao analisarNecessidade(Processo processo, String ufDestino, String paisDestino) {
        Objects.requireNonNull(processo, "processo");

        String paisNormalizado = normalizeCountry(paisDestino);
        String ufNormalizada = normalizeUpper(ufDestino);
        boolean internacional = isInternational(paisNormalizado);
        TriagemCooperacao triagem = construirTriagem(processo, ufNormalizada, paisNormalizado);
        NationalRulePackEngine.ResultadoRegras regras = avaliarRegras(processo, internacional, paisNormalizado, ufNormalizada);
        PrazoCalculado prazo = calcularPrazo(processo, triagem.tipoPreferencial(), internacional);

        List<String> etapas = new ArrayList<>(etapasEspecificasPorRamo(effectiveRamo(processo.getRamoDireito()), ufNormalizada));
        List<String> documentos = new ArrayList<>();
        List<String> alertas = new ArrayList<>(regras.alertas());
        String fundamento;
        int prazoEstimadoDias = prazo != null ? Math.max(1, prazo.diasCorridos()) : 30;
        boolean exigeHomologacaoSTJ = triagem.exigeHomologacaoSTJ();

        if (internacional) {
            PlanoCooperacao plano = montarPlanoInternacional(processo, paisNormalizado, triagem, prazo, regras);
            registrarAnalise(processo, plano, triagem);
            return plano;
        }

        RamoDireito ramo = effectiveRamo(processo.getRamoDireito());
        fundamento = resolverFundamentoNacional(ramo, triagem.tipoPreferencial());
        alertas.addAll(alertasEstruturaisNacionais(processo, triagem, prazo));
        alertas.addAll(extraAlertasPorRamo(ramo, false));

        if (triagem.recomendaVideoconferencia()) {
            etapas.add("Avaliar videoconferência ou ato eletrônico como substitutivo parcial da deprecação física");
        }
        if (prazo != null) {
            etapas.add("Prazo operacional sugerido até " + prazo.vencimento());
            alertas.addAll(prazo.advertencias());
        }
        if (ufNormalizada != null && !ufNormalizada.isBlank()) {
            etapas.add("Direcionar ao juízo ou unidade cooperante competente em " + ufNormalizada);
        }
        if (regras.bloqueante()) {
            alertas.add("Há requisitos bloqueantes no contexto do processo que exigem saneamento antes da expedição");
        }

        PlanoCooperacao plano = new PlanoCooperacao(
                triagem.tipoPreferencial(),
                etapas,
                documentos,
                alertas,
                fundamento,
                prazoEstimadoDias,
                exigeHomologacaoSTJ
        );
        registrarAnalise(processo, plano, triagem);
        return plano;
    }

    @Transactional
    public ResultadoExpedicao registrarExpedicao(Long processoId,
                                                 TipoCooperacao tipo,
                                                 String ufDestino,
                                                 String paisDestino,
                                                 String objeto,
                                                 List<String> documentosInformados) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(tipo, "tipo");
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));

        String paisDestinoCanonico = defaultIfBlank(normalizeCountry(paisDestino), "");
        String ufDestinoCanonica = defaultIfBlank(normalizeUpper(ufDestino), "");
        boolean internacional = isInternational(paisDestinoCanonico);
        String destinoPrincipal = resolveDestinoPrincipal(internacional, paisDestinoCanonico, ufDestinoCanonica);
        TriagemCooperacao triagem = construirTriagem(processo, ufDestinoCanonica, paisDestinoCanonico);
        PlanoCooperacao plano = analisarNecessidade(processo, ufDestino, paisDestino);
        List<String> documentos = new ArrayList<>(plano.documentosNecessarios());
        documentos.addAll(sanitizeStrings(documentosInformados));
        documentos = immutableDistinct(documentos);

        MatrizConformidade conformidade = avaliarConformidade(processo, tipo, objeto, documentos, internacional, triagem, plano);
        String numeroRastreio = gerarNumeroRastreio(processo, tipo, destinoPrincipal, internacional);
        UUID expedienteId = UUID.randomUUID();
        Instant registradaEm = Instant.now();

        processo.setStatusProcesso(resolveStatusAfterExpedicao(processo.getStatusProcesso()));
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        processoRepository.save(processo);

        registrarUi(processo, tipo, destinoPrincipal, numeroRastreio, internacional, conformidade);
        registrarAuditoria(processo, tipo, destinoPrincipal, numeroRastreio, conformidade, objeto);
        eventPublisher.publishEvent(new CooperacaoRegistradaEvent(
                processo.getId(),
                processo.getNumeroUnificado(),
                tipo,
                destinoPrincipal,
                numeroRastreio,
                registradaEm
        ));

        log.info("[Cooperacao] Expediente registrado: processo={} tipo={} destino={} rastreio={}",
                processo.getNumeroUnificado(), tipo, destinoPrincipal, numeroRastreio);

        return new ResultadoExpedicao(
                expedienteId,
                processo.getId(),
                tipo,
                conformidade.aptaExpedicao() ? StatusCooperacao.EXPEDIDA : StatusCooperacao.AGUARDANDO_APOSTILAMENTO,
                destinoPrincipal,
                numeroRastreio,
                internacional,
                triagem.exigeTraducao(),
                triagem.exigeApostilamento(),
                triagem.exigeHomologacaoSTJ(),
                mergeLists(plano.alertas(), conformidade.pendencias(), conformidade.travas()),
                conformidade.fingerprintIntegridade(),
                registradaEm
        );
    }

    public PainelCooperacao montarPainel(Long processoId, String ufDestino, String paisDestino) {
        Objects.requireNonNull(processoId, "processoId");
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        return montarPainel(processo, ufDestino, paisDestino);
    }

    public PainelCooperacao montarPainel(Processo processo, String ufDestino, String paisDestino) {
        Objects.requireNonNull(processo, "processo");
        PlanoCooperacao plano = analisarNecessidade(processo, ufDestino, paisDestino);
        String paisNormalizado = normalizeCountry(paisDestino);
        boolean internacional = isInternational(paisNormalizado);
        TriagemCooperacao triagem = construirTriagem(processo, normalizeUpper(ufDestino), paisNormalizado);
        MatrizConformidade conformidade = avaliarConformidade(
                processo,
                plano.tipoRecomendado(),
                processo.getAssunto(),
                plano.documentosNecessarios(),
                internacional,
                triagem,
                plano
        );

        List<String> indicadores = new ArrayList<>();
        indicadores.add("rota=" + triagem.tipoPreferencial().name());
        indicadores.add("canal=" + (triagem.canalPreferencial() != null ? triagem.canalPreferencial().name() : "NACIONAL"));
        indicadores.add("score_conformidade=" + round(conformidade.score()));
        indicadores.add("prazo=" + plano.prazoEstimadoDias() + " dias");
        if (triagem.recomendaVideoconferencia()) {
            indicadores.add("ato_virtual_recomendado=true");
        }
        if (plano.exigeHomologacaoSTJ()) {
            indicadores.add("homologacao_stj=true");
        }

        List<String> proximasAcoes = new ArrayList<>();
        proximasAcoes.add("Conferir documentos mínimos e destinatário correto");
        proximasAcoes.add("Emitir expediente com objeto detalhado e dados completos de diligência");
        if (triagem.exigeTraducao()) {
            proximasAcoes.add("Providenciar tradução juramentada integral");
        }
        if (triagem.exigeApostilamento()) {
            proximasAcoes.add("Providenciar apostilamento ou legalização consular aplicável");
        }
        if (triagem.recomendaVideoconferencia()) {
            proximasAcoes.add("Verificar possibilidade de oitiva ou citação por videoconferência assistida");
        }

        List<String> alertas = mergeLists(plano.alertas(), conformidade.pendencias(), conformidade.travas());
        String nivelOperacional = conformidade.aptaExpedicao()
                ? conformidade.score() >= 0.85 ? "PRONTO_PARA_EXPEDICAO" : "PRONTO_COM_RESSALVAS"
                : "PENDENTE_SANEAMENTO";

        String destinoPrincipal = resolveDestinoPrincipal(internacional, paisNormalizado, normalizeUpper(ufDestino));
        return new PainelCooperacao(
                processo.getNumeroUnificado(),
                plano.tipoRecomendado(),
                destinoPrincipal,
                internacional,
                nivelOperacional,
                indicadores,
                proximasAcoes,
                alertas,
                Instant.now()
        );
    }

    public List<String> gerarChecklistExpedicao(TipoCooperacao tipo, boolean internacional) {
        List<String> checklist = new ArrayList<>();
        checklist.add("Verificar competência do juízo requerente e da unidade destinatária");
        checklist.add("Descrever objeto, ato requerido, partes, endereços e prazo pretendido com precisão operacional");
        checklist.add("Conferir dados pessoais mínimos, preservando sigilo e necessidade de conhecer");
        checklist.add("Anexar peças essenciais, decisão base e informações logísticas para cumprimento");
        checklist.add("Gerar rastreio e registrar evento em trilha auditável e histórico operacional");
        if (internacional) {
            checklist.add("Validar tratado, autoridade central, canal diplomático ou via direta admissível");
            checklist.add("Providenciar tradução juramentada, apostilamento ou legalização conforme o país");
            checklist.add("Conferir se o pedido demanda homologação no STJ ou atuação do STF");
        } else {
            checklist.add("Conferir juízo deprecado, comarca, UF e integração eletrônica disponível");
            checklist.add("Avaliar videoconferência, central de mandados ou MNI/PJe antes de remessa física");
        }
        if (tipo == TipoCooperacao.PEDIDO_EXTRADICAO) {
            checklist.add("Separar elementos de nacionalidade, tipicidade, dupla incriminação e vedação constitucional");
        }
        if (tipo == TipoCooperacao.MLAT_MUTUA_ASSISTENCIA_LEGAL) {
            checklist.add("Adequar quesitos, cadeia de custódia e autoridade executora conforme tratado MLAT aplicável");
        }
        return immutableDistinct(checklist);
    }

    private PlanoCooperacao montarPlanoInternacional(Processo processo,
                                                     String pais,
                                                     TriagemCooperacao triagem,
                                                     PrazoCalculado prazo,
                                                     NationalRulePackEngine.ResultadoRegras regras) {
        RamoDireito ramo = effectiveRamo(processo.getRamoDireito());
        String fundamento = resolverFundamentoInternacional(triagem.tipoPreferencial(), ramo, pais);
        int prazoEstimadoDias = prazo != null ? Math.max(30, prazo.diasCorridos()) : 120;
        boolean homologacao = triagem.exigeHomologacaoSTJ();
        List<String> etapas = prazo != null
                ? new ArrayList<>(List.of("Prazo operacional sugerido até " + prazo.vencimento()))
                : new ArrayList<>();

        etapas.add("Mapear tratado, convenção ou reciprocidade operacional com " + defaultIfBlank(pais, DEFAULT_DESTINO));
        etapas.add("Definir canal preferencial: " + (triagem.canalPreferencial() != null ? triagem.canalPreferencial().descricao : "canal especializado"));
        etapas.add("Preparar expediente com narrativa fática, base normativa e delimitação do ato cooperacional");
        etapas.add("Controlar retorno, eventual devolução e reexpedição com saneamento de vícios formais");
        if (triagem.exigeTraducao()) {
            etapas.add("Providenciar tradução juramentada integral das peças essenciais");
        }
        if (triagem.exigeApostilamento()) {
            etapas.add("Providenciar apostilamento ou legalização consular antes do envio");
        }
        if (triagem.canalPreferencial() == CanaisCooperacaoInternacional.AUTORIDADE_CENTRAL_MJ) {
            etapas.add("Acionar DRCI/SENAJUS como Autoridade Central brasileira");
        }
        if (triagem.canalPreferencial() == CanaisCooperacaoInternacional.MINISTERIO_RELACOES_EXTERIORES) {
            etapas.add("Preparar fluxo diplomático via MRE quando a via jurisdicional direta não for possível");
        }
        if (triagem.tipoPreferencial() == TipoCooperacao.PEDIDO_EXTRADICAO) {
            etapas.add("Submeter material compatível com o regime constitucional de extradição e controle do STF");
            prazoEstimadoDias = Math.max(prazoEstimadoDias, 180);
        }
        if (triagem.tipoPreferencial() == TipoCooperacao.MLAT_MUTUA_ASSISTENCIA_LEGAL) {
            etapas.add("Adaptar quesitos de prova, diligência e cadeia de custódia ao tratado MLAT aplicável");
        }

        List<String> documentos = new ArrayList<>(documentosBaseInternacionais(processo, triagem.tipoPreferencial(), pais));
        List<String> alertas = new ArrayList<>(alertasEstruturaisInternacionais(processo, triagem, prazo, pais));
        alertas.addAll(extraAlertasPorRamo(ramo, true));
        if (prazo != null) {
            alertas.addAll(prazo.advertencias());
        }
        if (triagem.mercosul()) {
            alertas.add("Fluxo favorecido por instrumentos regionais do MERCOSUL, com tendência de menor fricção operacional");
            prazoEstimadoDias = Math.max(60, Math.min(prazoEstimadoDias, 120));
        }

        return new PlanoCooperacao(
                triagem.tipoPreferencial(),
                etapas,
                documentos,
                alertas,
                fundamento,
                prazoEstimadoDias,
                homologacao
        );
    }

    private TriagemCooperacao construirTriagem(Processo processo, String ufDestino, String paisDestino) {
        RamoDireito ramo = effectiveRamo(processo.getRamoDireito());
        String assunto = normalizeLower(processo.getAssunto());
        boolean internacional = isInternational(paisDestino);
        boolean mercosul = isMercosul(paisDestino);
        boolean altaUrgencia = ramo == RamoDireito.PENAL
                || ramo == RamoDireito.FAMILIA
                || ramo == RamoDireito.INFANCIA_JUVENTUDE
                || assunto.contains("liminar")
                || assunto.contains("urg")
                || assunto.contains("custod");
        boolean altaSensibilidade = processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO
                || ramo.geraSigiloAutomatico()
                || ramo == RamoDireito.PENAL
                || ramo == RamoDireito.INFANCIA_JUVENTUDE;
        boolean recomendaVideoconferencia = !internacional
                && (ramo == RamoDireito.PENAL || ramo == RamoDireito.TRABALHISTA || altaUrgencia)
                && ufDestino != null
                && !ufDestino.isBlank();

        TipoCooperacao tipoPreferencial;
        CanaisCooperacaoInternacional canalPreferencial = null;
        boolean exigeTraducao = false;
        boolean exigeApostilamento = false;
        boolean exigeHomologacaoSTJ = false;
        String racional;

        if (internacional) {
            exigeTraducao = true;
            exigeApostilamento = shouldApostille(paisDestino);
            if (containsAny(assunto, "extrad", "extradicao", "extradição")) {
                tipoPreferencial = TipoCooperacao.PEDIDO_EXTRADICAO;
                canalPreferencial = CanaisCooperacaoInternacional.INTERPOL_BRASIL;
                racional = "Tema de extradição com cooperação penal internacional reforçada";
            } else if (ramo == RamoDireito.PENAL || containsAny(assunto, "quebra de sigilo", "prova internacional", "mlat")) {
                tipoPreferencial = TipoCooperacao.MLAT_MUTUA_ASSISTENCIA_LEGAL;
                canalPreferencial = isUnitedStates(paisDestino)
                        ? CanaisCooperacaoInternacional.AUTORIDADE_CENTRAL_MJ
                        : mercosul
                        ? CanaisCooperacaoInternacional.MERCOSUL_LAS_LENAS
                        : CanaisCooperacaoInternacional.AUTORIDADE_CENTRAL_MJ;
                racional = "Cooperação probatória penal ou investigação transnacional";
            } else if (containsAny(assunto, "sentenca estrangeira", "sentença estrangeira", "homologacao", "homologação")) {
                tipoPreferencial = TipoCooperacao.CARTA_ROGATORIA_ATIVA;
                canalPreferencial = mercosul ? CanaisCooperacaoInternacional.MERCOSUL_LAS_LENAS : CanaisCooperacaoInternacional.MINISTERIO_RELACOES_EXTERIORES;
                exigeHomologacaoSTJ = true;
                racional = "Medida internacional com reflexo executório e necessidade de controle jurisdicional brasileiro";
            } else {
                tipoPreferencial = TipoCooperacao.AUXILIO_DIRETO_INTERNACIONAL;
                canalPreferencial = mercosul
                        ? CanaisCooperacaoInternacional.MERCOSUL_LAS_LENAS
                        : isPortugal(paisDestino)
                        ? CanaisCooperacaoInternacional.REDE_IBEROAMERICANA_COOPERACAO
                        : CanaisCooperacaoInternacional.AUTORIDADE_CENTRAL_MJ;
                racional = "Via internacional cooperacional com foco em celeridade e menor formalismo executório";
            }
        } else if (ramo == RamoDireito.PENAL) {
            tipoPreferencial = TipoCooperacao.CARTA_PRECATORIA_PENAL;
            racional = "Diligência penal interestadual ou intercomarcal";
        } else if (ramo == RamoDireito.TRABALHISTA) {
            tipoPreferencial = TipoCooperacao.CARTA_PRECATORIA_TRABALHISTA;
            racional = "Diligência trabalhista com territorialidade especializada";
        } else if (processo.getStatusProcesso() == StatusProcesso.CUMPRIMENTO_SENTENCA || containsAny(assunto, "penhora", "cumprimento", "execucao", "execução")) {
            tipoPreferencial = TipoCooperacao.CARTA_PRECATORIA_CUMPRIMENTO_SENTENCA;
            racional = "Atos executivos ou de constrição fora da jurisdição do juízo de origem";
        } else if (ufDestino != null && !ufDestino.isBlank() && processo.getJurisdicao() != null && !Objects.equals(normalizeUpper(processo.getJurisdicao().getEstado()), ufDestino)) {
            tipoPreferencial = TipoCooperacao.DEPRECADA_PJE_MNI;
            racional = "Troca eletrônica entre tribunais com ênfase em MNI/PJe";
        } else {
            tipoPreferencial = TipoCooperacao.CARTA_PRECATORIA_CIVEL;
            racional = "Diligência nacional ordinária fora da competência territorial imediata";
        }

        return new TriagemCooperacao(
                internacional,
                mercosul,
                altaUrgencia,
                altaSensibilidade,
                tipoPreferencial,
                canalPreferencial,
                recomendaVideoconferencia,
                exigeTraducao,
                exigeApostilamento,
                exigeHomologacaoSTJ,
                racional
        );
    }

    private MatrizConformidade avaliarConformidade(Processo processo,
                                                   TipoCooperacao tipo,
                                                   String objeto,
                                                   List<String> documentos,
                                                   boolean internacional,
                                                   TriagemCooperacao triagem,
                                                   PlanoCooperacao plano) {
        LinkedHashSet<String> pendencias = new LinkedHashSet<>();
        LinkedHashSet<String> travas = new LinkedHashSet<>();
        LinkedHashSet<String> salvaguardas = new LinkedHashSet<>();
        double score = 1.0;

        Set<String> docsUpper = new LinkedHashSet<>();
        for (String item : sanitizeStrings(documentos)) {
            docsUpper.add(normalizeUpper(item));
        }

        if (objeto == null || objeto.isBlank()) {
            pendencias.add("Objeto da cooperação não informado com precisão operacional");
            score -= 0.20;
        }
        if (processo.getNumeroUnificado() == null || processo.getNumeroUnificado().isBlank()) {
            travas.add("Processo sem número unificado consistente para cooperação");
            score -= 0.35;
        }
        if (internacional && triagem.exigeTraducao() && docsUpper.stream().noneMatch(d -> d.contains("TRADUCAO") || d.contains("TRADUÇÃO"))) {
            pendencias.add("Tradução juramentada ausente para expediente internacional");
            score -= 0.18;
        }
        if (internacional && triagem.exigeApostilamento() && docsUpper.stream().noneMatch(d -> d.contains("APOSTILA") || d.contains("LEGALIZACAO") || d.contains("LEGALIZAÇÃO"))) {
            pendencias.add("Apostilamento ou legalização internacional ainda não evidenciado");
            score -= 0.15;
        }
        if (triagem.altaSensibilidade()) {
            salvaguardas.add("Aplicar minimização de dados e restrição de acesso ao expediente cooperacional");
        }
        if (triagem.altaUrgencia()) {
            salvaguardas.add("Adotar canal mais célere disponível e monitoramento ativo do retorno");
        }
        if (plano.exigeHomologacaoSTJ()) {
            salvaguardas.add("Separar fluxo cooperacional do fluxo homologatório perante o STJ");
        }
        if (tipo == TipoCooperacao.PEDIDO_EXTRADICAO) {
            salvaguardas.add("Conferir nacionalidade, dupla tipicidade, especialidade e vedações constitucionais");
        }
        if (tipo == TipoCooperacao.MLAT_MUTUA_ASSISTENCIA_LEGAL) {
            salvaguardas.add("Preservar cadeia de custódia, hash de prova digital e autoridade executora competente");
        }

        String fingerprint = sha256(processo.getId() + "|" + processo.getNumeroUnificado() + "|" + tipo.name() + "|" + normalizeNullable(objeto) + "|" + String.join(";", docsUpper));
        boolean apta = travas.isEmpty() && score >= 0.55;
        return new MatrizConformidade(Math.max(0.0, Math.min(1.0, score)), new ArrayList<>(pendencias), new ArrayList<>(travas), new ArrayList<>(salvaguardas), apta, fingerprint);
    }

    private NationalRulePackEngine.ResultadoRegras avaliarRegras(Processo processo,
                                                                 boolean internacional,
                                                                 String paisDestino,
                                                                 String ufDestino) {
        GrauJurisdicao grau = processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : GrauJurisdicao.PRIMEIRO_GRAU;
        String tribunalCodigo = processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null;
        Map<String, Object> extras = new LinkedHashMap<>();
        extras.put("cooperacaoInternacional", internacional);
        extras.put("paisDestino", paisDestino);
        extras.put("ufDestino", ufDestino);
        extras.put("sigilo", processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : null);
        extras.put("valorCausa", processo.getValorCausa());
        extras.put("statusProcesso", processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : null);
        return rulePackEngine.aplicar(new NationalRulePackEngine.ContextoRegra(
                processo.getClasseProcessual(),
                processo.getAssunto(),
                effectiveRamo(processo.getRamoDireito()),
                grau,
                tribunalCodigo,
                extras
        ));
    }

    private PrazoCalculado calcularPrazo(Processo processo, TipoCooperacao tipo, boolean internacional) {
        RamoDireito ramo = effectiveRamo(processo.getRamoDireito());
        GrauJurisdicao grau = processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : GrauJurisdicao.PRIMEIRO_GRAU;
        String tribunalCodigo = processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : null;
        TipoPrazo tipoPrazo = switch (tipo) {
            case CARTA_PRECATORIA_PENAL, MLAT_MUTUA_ASSISTENCIA_LEGAL, PEDIDO_EXTRADICAO -> TipoPrazo.PRAZO_GENERICO;
            case CARTA_PRECATORIA_TRABALHISTA -> TipoPrazo.RECURSO_TRABALHISTA;
            case CARTA_PRECATORIA_CUMPRIMENTO_SENTENCA -> TipoPrazo.CUMPRIMENTO_SENTENCA;
            default -> internacional ? TipoPrazo.PRAZO_GENERICO : TipoPrazo.PRAZO_MP_MANIFESTACAO;
        };
        return prazoEngine.calcular(LocalDate.now(), tipoPrazo, ramo, grau, tribunalCodigo);
    }

    private void registrarAnalise(Processo processo, PlanoCooperacao plano, TriagemCooperacao triagem) {
        String payloadHash = sha256(processo.getNumeroUnificado() + "|" + plano.tipoRecomendado().name() + "|" + triagem.racional());
        auditLedger.appendSafely(
                "COOPERACAO_ANALISE_GERADA",
                RESOURCE_TYPE,
                safeProcessoId(processo),
                payloadHash,
                triagem.racional()
        );
    }

    private void registrarUi(Processo processo,
                             TipoCooperacao tipo,
                             String destinoPrincipal,
                             String numeroRastreio,
                             boolean internacional,
                             MatrizConformidade conformidade) {
        Usuario usuario = currentUserService.getOrNull();
        EnumSet<UiToken> tokens = EnumSet.of(UiToken.INFO, UiToken.DOCUMENTO);
        if (!conformidade.aptaExpedicao()) {
            tokens.add(UiToken.PENDENTE);
            tokens.add(UiToken.BLOQUEANTE);
        }
        if (internacional) {
            tokens.add(UiToken.ASSUNTO);
        }
        uiHistoryService.recordInboxEvent(
                inboxKey(processo),
                processo.getId(),
                "COOPERACAO_REGISTRADA",
                tokens,
                usuario != null ? usuario.getId() : null,
                usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : null,
                "Cooperação " + tipo.name() + " registrada para " + destinoPrincipal + " sob rastreio " + numeroRastreio
        );
    }

    private void registrarAuditoria(Processo processo,
                                    TipoCooperacao tipo,
                                    String destinoPrincipal,
                                    String numeroRastreio,
                                    MatrizConformidade conformidade,
                                    String objeto) {
        String justificativa = "tipo=" + tipo.name()
                + ",destino=" + destinoPrincipal
                + ",rastreio=" + numeroRastreio
                + ",score=" + round(conformidade.score())
                + ",objeto=" + safeShort(objeto);
        auditLedger.appendSafely(
                "COOPERACAO_EXPEDIDA",
                RESOURCE_TYPE,
                safeProcessoId(processo),
                conformidade.fingerprintIntegridade(),
                justificativa
        );
    }

    private StatusProcesso resolveStatusAfterExpedicao(StatusProcesso current) {
        if (current == null || current == StatusProcesso.PROTOCOLADO || current == StatusProcesso.DISTRIBUIDO) {
            return StatusProcesso.EM_ANDAMENTO;
        }
        return current;
    }

    private String resolverFundamentoNacional(RamoDireito ramo, TipoCooperacao tipo) {
        return switch (tipo) {
            case CARTA_PRECATORIA_PENAL -> "CPP arts. 353-368 + Resoluções CNJ sobre cooperação e videoconferência";
            case CARTA_PRECATORIA_TRABALHISTA -> "CLT art. 823 + CPC arts. 260-268 + interoperabilidade TRT";
            case CARTA_PRECATORIA_CUMPRIMENTO_SENTENCA -> "CPC arts. 513-538 + art. 516 parágrafo único";
            case AUXILIO_DIRETO_NACIONAL, DEPRECADA_PJE_MNI -> "CPC arts. 67-69, 260-268 + interoperabilidade CNJ/PJe/MNI";
            default -> switch (ramo) {
                case PENAL -> "CPP arts. 353-368";
                case TRABALHISTA -> "CLT art. 823 + CPC arts. 260-268";
                default -> "CPC arts. 260-268 + cooperação judiciária nacional";
            };
        };
    }

    private String resolverFundamentoInternacional(TipoCooperacao tipo, RamoDireito ramo, String pais) {
        if (tipo == TipoCooperacao.PEDIDO_EXTRADICAO) {
            return "CF art. 102, I, g + Lei 13.445/2017 + tratados de extradição aplicáveis";
        }
        if (tipo == TipoCooperacao.MLAT_MUTUA_ASSISTENCIA_LEGAL) {
            return isUnitedStates(pais)
                    ? "Decreto 3.810/2001 + CPP + tratados MLAT aplicáveis"
                    : "Tratados MLAT aplicáveis + Convenções de Mérida/Palermo quando pertinentes";
        }
        if (isPortugal(pais)) {
            return "CPC arts. 960-965 + Decreto 1.320/1994 + cooperação luso-brasileira";
        }
        if (isMercosul(pais)) {
            return "CPC arts. 960-965 + Decreto 2.067/1996 (Protocolo de Las Leñas)";
        }
        if (ramo == RamoDireito.PENAL) {
            return "CPP + CPC arts. 960-965 + tratados de cooperação penal internacional";
        }
        return "CPC arts. 960-965 + tratados, convenções e reciprocidade internacional aplicáveis";
    }

    private List<String> documentosBaseNacionais(Processo processo, TipoCooperacao tipo) {
        LinkedHashSet<String> docs = new LinkedHashSet<>();
        docs.add("PETICAO_OU_DECISAO_BASE");
        docs.add("QUALIFICACAO_COMPLETA_DAS_PARTES");
        docs.add("ENDERECO_VALIDADO_DESTINATARIO");
        docs.add("PECAS_ESSENCIAIS_DIGITAIS");
        if (tipo == TipoCooperacao.CARTA_PRECATORIA_CUMPRIMENTO_SENTENCA) {
            docs.add("TITULO_EXECUTIVO_OU_DECISAO_EXEQUENDA");
            docs.add("DEMONSTRATIVO_ATUALIZADO_DE_DEBITO");
        }
        if (effectiveRamo(processo.getRamoDireito()) == RamoDireito.PENAL) {
            docs.add("ROL_TESTEMUNHAS_OU_QUEBRA_DE_SIGILO_SE_PERTINENTE");
        }
        if (effectiveRamo(processo.getRamoDireito()) == RamoDireito.TRABALHISTA) {
            docs.add("ENDERECOS_E_LOCAIS_DE_TRABALHO_CONFIRMADOS");
        }
        return new ArrayList<>(docs);
    }

    private List<String> documentosBaseInternacionais(Processo processo, TipoCooperacao tipo, String pais) {
        LinkedHashSet<String> docs = new LinkedHashSet<>(documentosBaseNacionais(processo, tipo));
        docs.add("TRADUCAO_JURAMENTADA");
        docs.add("AUTORIDADE_DESTINATARIA_IDENTIFICADA");
        docs.add("DESCRICAO_PADRONIZADA_DA_DILIGENCIA");
        if (shouldApostille(pais)) {
            docs.add("APOSTILA_OU_LEGALIZACAO_CONSULAR");
        }
        if (tipo == TipoCooperacao.MLAT_MUTUA_ASSISTENCIA_LEGAL) {
            docs.add("QUESITOS_E_PARAMETROS_DE_PROVA_DIGITAL");
        }
        if (tipo == TipoCooperacao.PEDIDO_EXTRADICAO) {
            docs.add("MANDADO_OU_TITULO_CONDENATORIO");
            docs.add("DADOS_DE_NACIONALIDADE_E_DUPLA_INCRIMINACAO");
        }
        return new ArrayList<>(docs);
    }

    private List<String> etapasBaseNacionais(Processo processo, TriagemCooperacao triagem, String ufDestino) {
        List<String> etapas = new ArrayList<>();
        etapas.add("Validar unidade cooperante, competência territorial e endereço operacional do cumprimento");
        etapas.add("Gerar expediente eletrônico estruturado com peças essenciais e rastreamento único");
        etapas.add("Monitorar aceite, cumprimento, devolução e eventual reexpedição saneadora");
        if (ufDestino != null && !ufDestino.isBlank()) {
            etapas.add("Direcionar prioridade logística para a UF de destino " + ufDestino);
        }
        if (triagem.recomendaVideoconferencia()) {
            etapas.add("Verificar ato cooperacional por videoconferência para reduzir tempo de ciclo");
        }
        if (triagem.altaUrgencia()) {
            etapas.add("Marcar expediente com prioridade reforçada e acompanhamento ativo diário");
        }
        return etapas;
    }

    private List<String> etapasEspecificasPorRamo(RamoDireito ramo, String ufDestino) {
        List<String> etapas = new ArrayList<>();
        switch (ramo) {
            case PENAL -> {
                etapas.add("Priorizar oitiva remota, custódia de prova e cooperação com polícia judiciária quando couber");
                etapas.add("Certificar urgência em atos cautelares, testemunhais ou de localização");
            }
            case TRABALHISTA -> {
                etapas.add("Validar local da prestação de serviços e compatibilidade territorial do TRT ou vara cooperante");
                etapas.add("Ajustar diligência para citação, oitiva ou constrição patrimonial conforme a fase");
            }
            case FAMILIA, INFANCIA_JUVENTUDE -> {
                etapas.add("Preservar sigilo reforçado e comunicação sensível quando houver menores ou vulneráveis");
                etapas.add("Ajustar execução da diligência para evitar revitimização e exposição desnecessária");
            }
            case TRIBUTARIO, ADMINISTRATIVO -> etapas.add("Conferir destinatário institucional, órgão fazendário ou autoridade administrativa competente");
            case PREVIDENCIARIO -> etapas.add("Sincronizar a diligência com unidades federais, INSS ou juízo federal correspondente");
            case AMBIENTAL, AGRARIO -> etapas.add("Planejar diligência com suporte pericial, georreferenciamento e eventual apoio interinstitucional");
            default -> etapas.add("Adaptar a diligência ao objeto específico do ramo e ao nível de urgência do caso");
        }
        if (ufDestino != null && !ufDestino.isBlank() && (ramo == RamoDireito.AMBIENTAL || ramo == RamoDireito.AGRARIO)) {
            etapas.add("Mapear unidade ambiental, agrária ou fundiária cooperante na UF de destino " + ufDestino);
        }
        return etapas;
    }

    private List<String> alertasEstruturaisNacionais(Processo processo, TriagemCooperacao triagem, PrazoCalculado prazo) {
        List<String> alertas = new ArrayList<>();
        if (triagem.altaSensibilidade()) {
            alertas.add("Autos sensíveis exigem minimização de dados e controle de acesso no expediente cooperacional");
        }
        if (triagem.recomendaVideoconferencia()) {
            alertas.add("Ato remoto pode ser mais eficiente que expedição tradicional, dependendo da diligência");
        }
        if (prazo != null && prazo.diasCorridos() > 45) {
            alertas.add("Prazo operacional estimado elevado — considerar rota eletrônica prioritária e gestão ativa de SLA");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            alertas.add("Conferir mascaramento e compartilhamento mínimo de dados antes da expedição");
        }
        return alertas;
    }

    private List<String> alertasEstruturaisInternacionais(Processo processo,
                                                          TriagemCooperacao triagem,
                                                          PrazoCalculado prazo,
                                                          String pais) {
        List<String> alertas = new ArrayList<>();
        alertas.add("Confirmar existência de tratado, reciprocidade ou convenção operacional com " + defaultIfBlank(pais, DEFAULT_DESTINO));
        if (triagem.exigeTraducao()) {
            alertas.add("Documentação internacional requer revisão formal da tradução juramentada");
        }
        if (triagem.exigeApostilamento()) {
            alertas.add("Verificar apostilamento, legalização ou exigência consular específica do país requerido");
        }
        if (triagem.exigeHomologacaoSTJ()) {
            alertas.add("Separar a cooperação probatória do fluxo homologatório perante o STJ, quando aplicável");
        }
        if (prazo != null && prazo.diasCorridos() > 90) {
            alertas.add("Prazo internacional projetado longo — estabelecer checkpoint operacional periódico");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            alertas.add("Expediente internacional com sigilo exige remessa controlada e minimização de anexos pessoais");
        }
        return alertas;
    }

    private List<String> extraAlertasPorRamo(RamoDireito ramo, boolean internacional) {
        List<String> alertas = new ArrayList<>();
        switch (ramo) {
            case PENAL -> {
                alertas.add("Atos penais cooperacionais exigem precisão no objeto, autoridade executora e cadeia de custódia");
                if (internacional) {
                    alertas.add("Cooperação penal internacional não se confunde com extradição nem com homologação de sentença");
                }
            }
            case FAMILIA, INFANCIA_JUVENTUDE -> alertas.add("Evitar exposição de menores e dados íntimos em anexos compartilhados");
            case TRIBUTARIO -> alertas.add("Conferir requisitos de representação fazendária e regularidade do crédito ou ato administrativo");
            case AMBIENTAL, AGRARIO -> alertas.add("Planejar eventual diligência técnica, pericial ou apoio de órgãos especializados");
            case TRABALHISTA -> alertas.add("Conferir territorialidade da prestação laboral e unidade cooperante efetivamente competente");
            default -> {
            }
        }
        return alertas;
    }

    private String gerarNumeroRastreio(Processo processo, TipoCooperacao tipo, String destinoPrincipal, boolean internacional) {
        String prefixo = internacional ? "INT" : "NAC";
        String destino = normalizeAlnum(defaultIfBlank(destinoPrincipal, DEFAULT_DESTINO));
        String numero = normalizeAlnum(defaultIfBlank(processo.getNumeroUnificado(), String.valueOf(processo.getId())));
        String base = prefixo + "-" + tipo.name() + "-" + numero + "-" + destino;
        String hash = sha256(base).substring(0, 12).toUpperCase(Locale.ROOT);
        return prefixo + "-" + numero + "-" + hash;
    }

    private static String inboxKey(Processo processo) {
        return "COOPERACAO:" + (processo != null && processo.getId() != null ? processo.getId() : 0L);
    }

    private static String safeProcessoId(Processo processo) {
        return processo != null && processo.getId() != null ? String.valueOf(processo.getId()) : "0";
    }

    private static boolean isInternational(String paisDestino) {
        String token = normalizeUpper(paisDestino);
        return token != null && !token.isBlank() && !Set.of("BR", "BRA", "BRASIL").contains(token);
    }

    private static boolean isMercosul(String paisDestino) {
        String token = normalizeUpper(paisDestino);
        return Set.of("ARGENTINA", "PARAGUAI", "URUGUAI", "BOLIVIA").contains(token);
    }

    private static boolean isPortugal(String paisDestino) {
        String token = normalizeUpper(paisDestino);
        return "PORTUGAL".equals(token);
    }

    private static boolean isUnitedStates(String paisDestino) {
        String token = normalizeUpper(paisDestino);
        return Set.of("ESTADOS UNIDOS", "EUA", "USA", "UNITED STATES").contains(token);
    }

    private static boolean shouldApostille(String paisDestino) {
        String token = normalizeUpper(paisDestino);
        return token != null && !token.isBlank() && !Set.of("CANADA", "CANADÁ").contains(token);
    }

    private static RamoDireito effectiveRamo(RamoDireito ramo) {
        return ramo != null ? ramo : RamoDireito.CIVIL;
    }

    private static String normalizeCountry(String value) {
        String token = normalizeUpper(value);
        if (token == null) {
            return null;
        }
        return switch (token) {
            case "US", "USA", "UNITED STATES", "ESTADOSUNIDOS" -> "ESTADOS UNIDOS";
            case "PORTUGAL" -> "PORTUGAL";
            case "ARGENTINA" -> "ARGENTINA";
            case "PARAGUAI" -> "PARAGUAI";
            case "URUGUAI" -> "URUGUAI";
            case "BOLIVIA" -> "BOLIVIA";
            case "BR", "BRA", "BRASIL" -> "BRASIL";
            default -> value.trim();
        };
    }

    private static String normalizeUpper(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.isBlank() ? "" : normalized;
    }

    private static String resolveDestinoPrincipal(boolean internacional,
                                                 String paisNormalizado,
                                                 String ufDestinoNormalizada) {
        return internacional
                ? defaultIfBlank(paisNormalizado, DEFAULT_DESTINO)
                : defaultIfBlank(ufDestinoNormalizada, DEFAULT_DESTINO);
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean containsAny(String text, String... terms) {
        String base = normalizeLower(text);
        for (String term : terms) {
            if (term != null && !term.isBlank() && base.contains(normalizeLower(term))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> sanitizeStrings(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String value : values) {
            String normalized = value.trim();
            if (!normalized.isBlank()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private static List<String> immutableDistinct(Collection<String> values) {
        return List.copyOf(new LinkedHashSet<>(sanitizeStrings(values)));
    }

    @SafeVarargs
    private static List<String> mergeLists(Collection<String>... collections) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (collections != null) {
            for (Collection<String> collection : collections) {
                merged.addAll(sanitizeStrings(collection));
            }
        }
        return List.copyOf(merged);
    }

    private static String normalizeAlnum(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.isEmpty() ? "NA" : sb.toString();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String safeShort(String text) {
        String normalized = normalizeNullable(text);
        return normalized.length() > 180 ? normalized.substring(0, 180) : normalized;
    }
}
