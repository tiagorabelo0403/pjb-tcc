package com.tcc.pjb.backend.platform.jusos.v2.impedimento;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
public class ImpedimentoSuspeicaoEngine {

    private static final String RESOURCE_TYPE_PROCESSO = "PROCESSO";
    private static final String RESOURCE_TYPE_COMPLIANCE = "IMPEDIMENTO_SUSPEICAO";

    public enum TipoConflito {
        IMPEDIMENTO_LEGAL,
        SUSPEICAO_SUBJETIVA,
        CONFLITO_INTERESSE_PERITO,
        ADVOGADO_MAGISTRADO_MESMO_ESCRITORIO,
        PARENTESCO_PARTE,
        INTERESSE_ECONOMICO_DIRETO,
        ATUACAO_ANTERIOR_CAUSA,
        IDENTIDADE_COM_PARTE,
        IDENTIDADE_COM_PATRONO,
        LACO_SOCIOPROFISSIONAL_LOCAL,
        CONTATO_INSTITUCIONAL_SENSIVEL,
        SIGILO_REFORCADO_COM_CONFLITO
    }

    public enum StatusDeclaracao {
        DECLARADA_ESPONTANEAMENTE,
        ARGUIDA_POR_PARTE,
        AFASTADA_PELO_PROPRIO,
        JULGADA_PROCEDENTE,
        JULGADA_IMPROCEDENTE,
        PENDENTE_ANALISE,
        SUBMETIDA_A_GOVERNANCA,
        ENCAMINHADA_A_CORREGEDORIA
    }

    public enum FundamentoLegal {
        CPC_ART_144_IMPEDIMENTO("CPC art. 144 — Impedimento do juiz"),
        CPC_ART_145_SUSPEICAO("CPC art. 145 — Suspeição do juiz"),
        CPP_ART_252_IMPEDIMENTO("CPP art. 252 — Impedimento penal"),
        CPP_ART_254_SUSPEICAO("CPP art. 254 — Suspeição penal"),
        CLT_ART_801_EXCECAO("CLT art. 801 — Exceção de suspeição trabalhista"),
        LOMAN_DEVER_ABSTENCAO("LOMAN art. 36 — Dever de abstenção"),
        RESOLUCAO_CNJ_7_NEPOTISMO("Res. CNJ 7/2005 — Nepotismo e vedação correlata"),
        CODIGO_ETICA_OAB("Código de Ética da OAB — conflito de interesses"),
        CPC_ART_148_AUXILIARES("CPC art. 148 — auxiliares da justiça e demais sujeitos imparciais"),
        LEI_9784_IMPARCIALIDADE("Lei 9.784/1999 — imparcialidade administrativa"),
        RECOMENDACAO_CNJ_GOVERNANCA("Boas práticas CNJ de governança, integridade e prevenção de conflitos") ;

        public final String descricao;

        FundamentoLegal(String descricao) {
            this.descricao = descricao;
        }
    }

    public record DeclaracaoConflito(
            UUID declaracaoId,
            Long processoId,
            Long usuarioId,
            String usuarioNome,
            String usuarioPapel,
            TipoConflito tipo,
            FundamentoLegal fundamento,
            String descricaoConflito,
            StatusDeclaracao status,
            boolean declaradaEspontaneamente,
            Long arguantePorId,
            Instant declaradaEm,
            Instant resolvidaEm,
            String despachoResolutivo,
            MatrizConflito matriz,
            List<String> anexosLogicos,
            List<String> blindagensAplicadas
    ) {
        public DeclaracaoConflito {
            declaracaoId = declaracaoId != null ? declaracaoId : UUID.randomUUID();
            anexosLogicos = immutableDistinct(anexosLogicos);
            blindagensAplicadas = immutableDistinct(blindagensAplicadas);
        }
    }

    public record ResultadoVerificacao(
            boolean conflitosDetectados,
            List<ConflitoPotencial> conflitos,
            List<String> alertas,
            boolean exigeAfastamento,
            boolean exigeDeclaracao,
            String acaoRecomendada,
            MatrizConflito matriz,
            List<String> checklistGovernanca,
            List<String> blindagensSugeridas,
            List<String> referenciasContextuais
    ) {
        public ResultadoVerificacao {
            conflitos = immutableDistinctConflitos(conflitos);
            alertas = immutableDistinct(alertas);
            checklistGovernanca = immutableDistinct(checklistGovernanca);
            blindagensSugeridas = immutableDistinct(blindagensSugeridas);
            referenciasContextuais = immutableDistinct(referenciasContextuais);
        }

        public boolean temConflitoCritico() {
            return conflitos.stream().anyMatch(c -> c.gravidadeEstimada() >= 0.85d);
        }
    }

    public record ConflitoPotencial(
            TipoConflito tipo,
            FundamentoLegal fundamento,
            String descricao,
            String parteEnvolvida,
            double gravidadeEstimada,
            boolean bloqueante,
            String evidenciaChave,
            List<String> diligenciasRecomendadas
    ) {
        public ConflitoPotencial {
            gravidadeEstimada = clamp(gravidadeEstimada);
            diligenciasRecomendadas = immutableDistinct(diligenciasRecomendadas);
        }
    }

    public record MatrizConflito(
            int scoreMaterialidade,
            int scoreProbabilidade,
            int scoreGovernanca,
            double gravidadeAgregada,
            String classificacao,
            List<String> fatoresDeterminantes,
            List<String> provasMinimas,
            List<String> travasOperacionais
    ) {
        public MatrizConflito {
            gravidadeAgregada = clamp(gravidadeAgregada);
            fatoresDeterminantes = immutableDistinct(fatoresDeterminantes);
            provasMinimas = immutableDistinct(provasMinimas);
            travasOperacionais = immutableDistinct(travasOperacionais);
        }
    }

    public record PainelConformidade(
            Long processoId,
            String numeroUnificado,
            int totalAgentesAvaliados,
            int totalConflitosCriticos,
            int totalConflitosModerados,
            boolean exigeGovernancaImediata,
            List<String> gargalos,
            List<String> oportunidadesControle,
            List<String> proximosPassos
    ) {
        public PainelConformidade {
            gargalos = immutableDistinct(gargalos);
            oportunidadesControle = immutableDistinct(oportunidadesControle);
            proximosPassos = immutableDistinct(proximosPassos);
        }
    }

    public record ConflitoVerificadoEvent(
            Long processoId,
            String numeroUnificado,
            Long usuarioAvaliadoId,
            String usuarioAvaliadoNome,
            boolean conflitosDetectados,
            boolean exigeAfastamento,
            String classificacao,
            Instant verificadoEm
    ) {}

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLedgerService auditLedger;
    private final CurrentUserService currentUserService;
    private final UiHistoryService uiHistoryService;
    private final ApplicationEventPublisher eventPublisher;
    private final NationalRulePackEngine rulePackEngine;

    public ImpedimentoSuspeicaoEngine(
            ProcessoRepository processoRepository,
            UsuarioRepository usuarioRepository,
            AuditLedgerService auditLedger,
            CurrentUserService currentUserService,
            UiHistoryService uiHistoryService,
            ApplicationEventPublisher eventPublisher,
            NationalRulePackEngine rulePackEngine
    ) {
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "usuarioRepository");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService, "uiHistoryService");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.rulePackEngine = Objects.requireNonNull(rulePackEngine, "rulePackEngine");
    }

    public ResultadoVerificacao verificarConflito(Usuario magistradoOuServidor, Processo processo) {
        Objects.requireNonNull(magistradoOuServidor, "usuario");
        Objects.requireNonNull(processo, "processo");

        List<ConflitoPotencial> conflitos = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        List<String> referencias = new ArrayList<>();

        RamoDireito ramo = processo.getRamoDireito();
        GrauJurisdicao grau = processo.getJurisdicao() != null && processo.getJurisdicao().getGrau() != null
                ? processo.getJurisdicao().getGrau()
                : GrauJurisdicao.PRIMEIRO_GRAU;

        verificarIdentidadeComParte(magistradoOuServidor, processo, conflitos, alertas);
        verificarPatrocinioOuAtuacaoAnterior(magistradoOuServidor, processo, conflitos, alertas);
        verificarParentescoIndiciario(magistradoOuServidor, processo, conflitos, alertas);
        verificarInteresseEconomico(magistradoOuServidor, processo, conflitos, alertas);
        verificarEscritorioAdvogado(magistradoOuServidor, processo, conflitos, alertas);
        verificarPericiaEAssistenciaTecnica(magistradoOuServidor, processo, conflitos, alertas);
        verificarSensibilidadeInstitucional(magistradoOuServidor, processo, conflitos, alertas);
        incorporarContextoNormativo(processo, referencias, alertas);

        MatrizConflito matriz = construirMatriz(magistradoOuServidor, processo, conflitos, alertas);
        List<String> checklist = new ArrayList<>(gerarChecklistPreventivo(ramo, grau, processo.getNivelSigilo()));
        List<String> blindagens = new ArrayList<>(sugerirBlindagens(magistradoOuServidor, processo, conflitos, matriz));

        boolean exigeAfastamento = conflitos.stream().anyMatch(c -> c.bloqueante() || c.gravidadeEstimada() >= 0.90d)
                || matriz.gravidadeAgregada() >= 0.90d;
        boolean exigeDeclaracao = !conflitos.isEmpty() || matriz.gravidadeAgregada() >= 0.45d;

        String acao = exigeAfastamento
                ? "AFASTAMENTO_IMEDIATO_E_REDISTRIBUICAO_CONTROLADA"
                : exigeDeclaracao
                ? "DECLARACAO_FORMAL_E_SUBMISSAO_A_GOVERNANCA"
                : "MANTER_MONITORAMENTO_COM_REGISTRO_PREVENTIVO";

        if (exigeAfastamento) {
            alertas.add("Conflito com potencial invalidante detectado. Impede prosseguimento sem substituição formal.");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO && exigeDeclaracao) {
            blindagens.add("Restringir visualização do incidente a usuários autorizados e corregedoria interna.");
        }

        ResultadoVerificacao resultado = new ResultadoVerificacao(
                !conflitos.isEmpty(),
                conflitos,
                alertas,
                exigeAfastamento,
                exigeDeclaracao,
                acao,
                matriz,
                checklist,
                blindagens,
                referencias
        );

        registrarAuditoria(magistradoOuServidor, processo, resultado);
        publicarSinais(magistradoOuServidor, processo, resultado);
        return resultado;
    }

    public ResultadoVerificacao verificarConflitoPorIds(Long usuarioId, Long processoId) {
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(processoId, "processoId");
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        return verificarConflito(usuario, processo);
    }

    public PainelConformidade gerarPainelConformidade(Processo processo, List<Usuario> agentes) {
        Objects.requireNonNull(processo, "processo");
        List<Usuario> avaliados = agentes == null || agentes.isEmpty()
                ? inferirAgentesRelacionados(processo)
                : agentes.stream().filter(Objects::nonNull).toList();

        List<String> gargalos = new ArrayList<>();
        List<String> oportunidades = new ArrayList<>();
        List<String> proximos = new ArrayList<>();
        int criticos = 0;
        int moderados = 0;

        for (Usuario agente : avaliados) {
            ResultadoVerificacao r = verificarConflito(agente, processo);
            if (r.temConflitoCritico()) {
                criticos++;
            } else if (r.conflitosDetectados()) {
                moderados++;
            }
        }

        if (criticos > 0) {
            gargalos.add("Há conflito crítico com potencial de nulidade absoluta ou quebra de imparcialidade.");
            proximos.add("Acionar substituição formal do agente e preservar trilha auditável do incidente.");
        }
        if (moderados > 0) {
            gargalos.add("Existem conflitos moderados que exigem declaração, triagem ou validação corregedora.");
            proximos.add("Consolidar evidências documentais antes de manter o agente na causa.");
        }
        if (criticos == 0 && moderados == 0) {
            oportunidades.add("Processo apto a rodar monitoramento preventivo sem bloqueio imediato.");
            proximos.add("Registrar revisão preventiva periódica em atos sensíveis e sustentações orais.");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            oportunidades.add("Aplicar segregação de acesso e fila restrita para incidentes de impedimento em autos sigilosos.");
        }
        if (processo.getRamoDireito() == RamoDireito.FAMILIA || processo.getRamoDireito() == RamoDireito.INFANCIA_JUVENTUDE) {
            oportunidades.add("Executar dupla revisão humana para prevenir contaminação decisória em casos sensíveis.");
        }

        return new PainelConformidade(
                processo.getId(),
                processo.getNumeroUnificado(),
                avaliados.size(),
                criticos,
                moderados,
                criticos > 0,
                gargalos,
                oportunidades,
                proximos
        );
    }

    public List<String> gerarRoteiroDeclararImpedimento(TipoConflito tipo, RamoDireito ramo) {
        List<String> roteiro = new ArrayList<>();
        FundamentoLegal fundamento = resolverFundamento(tipo, ramo);

        roteiro.add("Identificar a causa objetiva ou subjetiva e registrar evidências mínimas de suporte.");
        roteiro.add("Produzir despacho de abstenção ou de remessa com fundamento em: " + fundamento.descricao + ".");
        roteiro.add("Suspender distribuição interna de novos atos ao agente até saneamento do incidente.");
        roteiro.add("Remeter os autos ao substituto legal ou órgão revisor competente, preservando cadeia de custódia dos atos digitais.");
        roteiro.add("Intimar partes e órgãos essenciais sobre a substituição, com rastreabilidade da decisão de governança.");
        roteiro.add("Registrar o incidente em trilha imutável de auditoria e histórico operacional da UI.");

        if (tipo == TipoConflito.IMPEDIMENTO_LEGAL || tipo == TipoConflito.IDENTIDADE_COM_PARTE || tipo == TipoConflito.ATUACAO_ANTERIOR_CAUSA) {
            roteiro.add("Tratar como vício de máxima criticidade, com revisão dos atos potencialmente contaminados.");
        } else {
            roteiro.add("Abrir janela para manifestação das partes e validação pela chefia/corregedoria, conforme o rito aplicável.");
        }

        if (ramo == RamoDireito.PENAL) {
            roteiro.add("Comunicar o órgão colegiado/presidência para saneamento célere de prisão, cautelares ou audiência penal.");
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            roteiro.add("Tratar prioridade de pauta e reencaixe processual para evitar nulidade em audiência una ou execução alimentar.");
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            roteiro.add("Resguardar dados sensíveis e limitar a publicidade do incidente ao mínimo necessário.");
        }
        return List.copyOf(roteiro);
    }

    public DeclaracaoConflito registrarDeclaracao(
            Long processoId,
            Long usuarioId,
            String papel,
            TipoConflito tipo,
            String descricao,
            boolean espontanea
    ) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(tipo, "tipo");

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));

        ResultadoVerificacao verificacao = verificarConflito(usuario, processo);
        DeclaracaoConflito declaracao = new DeclaracaoConflito(
                UUID.randomUUID(),
                processoId,
                usuarioId,
                safe(usuario.getNome()),
                normalizeRole(papel != null ? papel : usuario.getPerfil()),
                tipo,
                resolverFundamento(tipo, processo.getRamoDireito()),
                safe(descricao),
                espontanea ? StatusDeclaracao.DECLARADA_ESPONTANEAMENTE : StatusDeclaracao.ARGUIDA_POR_PARTE,
                espontanea,
                Optional.ofNullable(currentUserService.getOrNull()).map(Usuario::getId).orElse(null),
                Instant.now(),
                null,
                null,
                verificacao.matriz(),
                anexosLogicos(verificacao),
                verificacao.blindagensSugeridas()
        );

        String payloadHash = sha256Hex(String.join("|",
                String.valueOf(processoId),
                String.valueOf(usuarioId),
                tipo.name(),
                String.valueOf(declaracao.declaradaEm()),
                safe(descricao)));

        auditLedger.appendSafely(
                "DECLARACAO_CONFLITO_REGISTRADA",
                RESOURCE_TYPE_COMPLIANCE,
                declaracao.declaracaoId().toString(),
                payloadHash,
                "Incidente de impedimento/suspeição registrado no fluxo nacional"
        );

        registrarInbox(processo, declaracao.status().name(), verificacao.temConflitoCritico(), verificacao.exigeDeclaracao());
        eventPublisher.publishEvent(new ConflitoVerificadoEvent(
                processo.getId(),
                processo.getNumeroUnificado(),
                usuario.getId(),
                safe(usuario.getNome()),
                verificacao.conflitosDetectados(),
                verificacao.exigeAfastamento(),
                verificacao.matriz().classificacao(),
                Instant.now()
        ));

        return declaracao;
    }

    public DeclaracaoConflito resolverDeclaracao(DeclaracaoConflito declaracao, StatusDeclaracao statusFinal, String despachoResolutivo) {
        Objects.requireNonNull(declaracao, "declaracao");
        Objects.requireNonNull(statusFinal, "statusFinal");

        DeclaracaoConflito resolvida = new DeclaracaoConflito(
                declaracao.declaracaoId(),
                declaracao.processoId(),
                declaracao.usuarioId(),
                declaracao.usuarioNome(),
                declaracao.usuarioPapel(),
                declaracao.tipo(),
                declaracao.fundamento(),
                declaracao.descricaoConflito(),
                statusFinal,
                declaracao.declaradaEspontaneamente(),
                declaracao.arguantePorId(),
                declaracao.declaradaEm(),
                Instant.now(),
                safe(despachoResolutivo),
                declaracao.matriz(),
                declaracao.anexosLogicos(),
                declaracao.blindagensAplicadas()
        );

        auditLedger.appendSafely(
                "DECLARACAO_CONFLITO_RESOLVIDA",
                RESOURCE_TYPE_COMPLIANCE,
                resolvida.declaracaoId().toString(),
                sha256Hex(resolvida.status().name() + "|" + safe(despachoResolutivo)),
                safe(despachoResolutivo)
        );

        return resolvida;
    }

    public List<String> gerarChecklistPreventivo(RamoDireito ramo, GrauJurisdicao grau, NivelSigilo nivelSigilo) {
        List<String> checklist = new ArrayList<>();
        checklist.add("Validar CPF, OAB e vínculo funcional do agente antes da distribuição ou do ato decisório.");
        checklist.add("Rever histórico de atuação anterior do agente no mesmo número unificado e em incidentes conexos.");
        checklist.add("Comparar parte autora, parte ré, patrono principal e usuário vinculado ao processo com a base interna de usuários.");
        checklist.add("Registrar autodeclaração de ausência de conflito para atos sensíveis, sessões colegiadas e perícias críticas.");
        checklist.add("Acionar dupla validação humana quando o processo envolver sigilo reforçado, vulneráveis ou impacto institucional alto.");
        checklist.add("Persistir auditoria imutável de toda triagem de conflito, inclusive verificações negativas.");

        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            checklist.add("Executar triagem reforçada antes de decisões cautelares, custódia, pronúncia e atos de investigação sensível.");
        }
        if (ramo == RamoDireito.FAMILIA || ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            checklist.add("Avaliar relação prévia com as partes e restringir publicidade interna do incidente ao mínimo necessário.");
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU || grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) {
            checklist.add("Validar impedimento de relator, revisor, presidente e gabinetes associados antes da inclusão em pauta.");
        }
        if (nivelSigilo != null && nivelSigilo != NivelSigilo.PUBLICO) {
            checklist.add("Separar o incidente de conflito em trilha restrita, sem exposição indevida de partes e fundamento sensível.");
        }
        return List.copyOf(checklist);
    }

    private void verificarIdentidadeComParte(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, List<String> alertas) {
        String cpfUsuario = digitsOnly(usuario.getCpf());
        if (cpfUsuario.isBlank()) {
            alertas.add("Usuário sem CPF normalizado na base interna. Triagem automática perde precisão.");
            return;
        }
        if (cpfUsuario.equals(digitsOnly(processo.getParteAutoraCpf()))) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.IDENTIDADE_COM_PARTE,
                    FundamentoLegal.CPC_ART_144_IMPEDIMENTO,
                    "O agente coincide com a parte autora do processo.",
                    safe(processo.getParteAutoraNome()),
                    1.00d,
                    true,
                    "CPF do agente coincide com CPF da parte autora.",
                    List.of("Confirmar cadastro da parte autora", "Remeter imediatamente ao substituto legal")
            ));
        }
        if (cpfUsuario.equals(digitsOnly(processo.getParteReuCpf()))) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.IDENTIDADE_COM_PARTE,
                    FundamentoLegal.CPC_ART_144_IMPEDIMENTO,
                    "O agente coincide com a parte ré do processo.",
                    safe(processo.getParteReuNome()),
                    1.00d,
                    true,
                    "CPF do agente coincide com CPF da parte ré.",
                    List.of("Confirmar cadastro da parte ré", "Revisar todos os atos já praticados")
            ));
        }
    }

    private void verificarPatrocinioOuAtuacaoAnterior(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, List<String> alertas) {
        Usuario vinculado = processo.getUsuario();
        if (vinculado == null) {
            return;
        }
        if (Objects.equals(usuario.getId(), vinculado.getId())) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.ATUACAO_ANTERIOR_CAUSA,
                    resolverFundamento(TipoConflito.ATUACAO_ANTERIOR_CAUSA, processo.getRamoDireito()),
                    "O agente avaliado já figura como usuário responsável ou vinculado ao processo.",
                    safe(vinculado.getNome()),
                    0.98d,
                    true,
                    "Identificador interno do agente coincide com o usuário vinculado ao processo.",
                    List.of("Verificar natureza do vínculo anterior", "Reatribuir o feito a agente não contaminado")
            ));
            return;
        }
        if (digitsOnly(usuario.getCpf()).equals(digitsOnly(vinculado.getCpf()))) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.IDENTIDADE_COM_PATRONO,
                    FundamentoLegal.CODIGO_ETICA_OAB,
                    "O agente coincide documentalmente com o patrono/usuário vinculado ao processo.",
                    safe(vinculado.getNome()),
                    0.95d,
                    true,
                    "CPF do agente coincide com CPF do usuário vinculado ao processo.",
                    List.of("Checar atuação pretérita em petições", "Bloquear distribuição até saneamento")
            ));
        }
        if (sameNonBlank(usuario.getOabNormalizada(), vinculado.getOabNormalizada())) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.ADVOGADO_MAGISTRADO_MESMO_ESCRITORIO,
                    FundamentoLegal.CODIGO_ETICA_OAB,
                    "Há coincidência de OAB normalizada entre o agente e o usuário vinculado ao processo.",
                    safe(vinculado.getNome()),
                    0.92d,
                    true,
                    "OAB normalizada coincide com o patrono vinculado ao processo.",
                    List.of("Validar eventual mesma banca/escritório", "Submeter o incidente à governança funcional")
            ));
        }
    }

    private void verificarParentescoIndiciario(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, List<String> alertas) {
        String sobrenomeUsuario = lastToken(usuario.getNome());
        if (sobrenomeUsuario.isBlank()) {
            return;
        }
        boolean mesmaComarca = sameNonBlank(usuario.getComarca(), processo.getJurisdicao() != null ? processo.getJurisdicao().getCidade() : null);
        if (mesmaComarca && sharesSurname(usuario.getNome(), processo.getParteAutoraNome())) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.PARENTESCO_PARTE,
                    FundamentoLegal.CPC_ART_145_SUSPEICAO,
                    "Há indício nominal e territorial de proximidade familiar/social com a parte autora.",
                    safe(processo.getParteAutoraNome()),
                    0.62d,
                    false,
                    "Sobrenome relevante e mesma comarca do processo.",
                    List.of("Conferir parentesco até o 3º grau", "Solicitar autodeclaração formal do agente")
            ));
        }
        if (mesmaComarca && sharesSurname(usuario.getNome(), processo.getParteReuNome())) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.PARENTESCO_PARTE,
                    FundamentoLegal.CPC_ART_145_SUSPEICAO,
                    "Há indício nominal e territorial de proximidade familiar/social com a parte ré.",
                    safe(processo.getParteReuNome()),
                    0.62d,
                    false,
                    "Sobrenome relevante e mesma comarca do processo.",
                    List.of("Conferir parentesco até o 3º grau", "Validar eventual convivência ou sociedade")
            ));
        }
        if (mesmaComarca) {
            alertas.add("Triagem local detectou coincidência territorial. Confirme parentesco, sociedade ou amizade íntima por declaração funcional.");
        }
    }

    private void verificarInteresseEconomico(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, List<String> alertas) {
        if (processo.getValorCausa() == null) {
            return;
        }
        if (processo.getValorCausa().compareTo(new java.math.BigDecimal("1000000")) >= 0) {
            alertas.add("Valor da causa elevado. Reforçar triagem de interesse econômico direto ou indireto do agente e pessoas próximas.");
        }
        if (usuario.getTipoUsuario() != null && (usuario.getTipoUsuario().isProcuradoria() || usuario.isMinisterioPublico())) {
            alertas.add("Agente institucional com potencial interesse reflexo. Validar se atua no mesmo ente ou política pública objeto da causa.");
        }
        if (processo.getRamoDireito() == RamoDireito.TRIBUTARIO || processo.getRamoDireito() == RamoDireito.ADMINISTRATIVO) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.INTERESSE_ECONOMICO_DIRETO,
                    FundamentoLegal.LEI_9784_IMPARCIALIDADE,
                    "Ramo com alta sensibilidade institucional e potencial de interesse econômico ou arrecadatório reflexo.",
                    safe(processo.getNumeroUnificado()),
                    processo.getValorCausa() != null && processo.getValorCausa().compareTo(new java.math.BigDecimal("5000000")) >= 0 ? 0.74d : 0.46d,
                    false,
                    "Causa em matéria tributária/administrativa com reflexo patrimonial relevante.",
                    List.of("Mapear vínculo com ente público interessado", "Reforçar aprovação por autoridade revisora")
            ));
        }
    }

    private void verificarEscritorioAdvogado(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, List<String> alertas) {
        Usuario patrono = processo.getUsuario();
        if (patrono == null) {
            return;
        }
        if (usuario.isAdvogado() && patrono.isAdvogado() && sameNonBlank(usuario.getOabUf(), patrono.getOabUf())) {
            alertas.add("Agente e patrono pertencem à mesma praça profissional. Verificar sociedade, compartilhamento de banca ou vínculo econômico.");
        }
        if (usuario.isMagistrado() && patrono.isAdvogado() && sameNonBlank(lastToken(usuario.getNome()), lastToken(patrono.getNome()))) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.ADVOGADO_MAGISTRADO_MESMO_ESCRITORIO,
                    FundamentoLegal.RESOLUCAO_CNJ_7_NEPOTISMO,
                    "Há indicativo de proximidade profissional ou familiar entre magistrado e patrono vinculado.",
                    safe(patrono.getNome()),
                    0.67d,
                    false,
                    "Coincidência de praça profissional e sobrenome relevante.",
                    List.of("Conferir sociedade de fato ou parentesco", "Afastar o magistrado se confirmada proximidade vedada")
            ));
        }
    }

    private void verificarPericiaEAssistenciaTecnica(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, List<String> alertas) {
        if (!isPericiaSensivel(usuario.getTipoUsuario())) {
            return;
        }
        FundamentoLegal fundamento = FundamentoLegal.CPC_ART_148_AUXILIARES;
        if (digitsOnly(usuario.getCpf()).equals(digitsOnly(processo.getParteAutoraCpf()))
                || digitsOnly(usuario.getCpf()).equals(digitsOnly(processo.getParteReuCpf()))) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.CONFLITO_INTERESSE_PERITO,
                    fundamento,
                    "Perito ou auxiliar coincide com parte diretamente envolvida.",
                    safe(usuario.getNome()),
                    1.00d,
                    true,
                    "CPF do perito/auxiliar coincide com o de parte cadastrada.",
                    List.of("Substituir imediatamente o auxiliar", "Comunicar o juízo sobre nulidade potencial da prova")
            ));
            return;
        }
        if (processo.getRamoDireito() == RamoDireito.PREVIDENCIARIO || processo.getRamoDireito() == RamoDireito.FAMILIA) {
            alertas.add("Perícia em matéria sensível exige declaração reforçada de independência técnica e ausência de contato privado com as partes.");
        }
    }

    private void verificarSensibilidadeInstitucional(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, List<String> alertas) {
        RamoDireito ramo = processo.getRamoDireito();
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR || ramo == RamoDireito.ELEITORAL) {
            alertas.add("Matéria de alta sensibilidade institucional. Reforce segregação de funções, registro de contatos e revisão por superior imediato.");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            conflitos.add(new ConflitoPotencial(
                    TipoConflito.SIGILO_REFORCADO_COM_CONFLITO,
                    FundamentoLegal.RECOMENDACAO_CNJ_GOVERNANCA,
                    "Autos sigilosos elevam o impacto operacional de qualquer suspeita de parcialidade.",
                    safe(processo.getNumeroUnificado()),
                    0.51d,
                    false,
                    "O processo possui nível de sigilo diferente de público.",
                    List.of("Restringir incidente a fila interna protegida", "Evitar compartilhamento aberto do fundamento sensível")
            ));
        }
        if (usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR || usuario.getTipoUsuario() == TipoUsuario.MINISTRO) {
            alertas.add("Agente em posição colegiada/superior. Validar também impedimento de gabinete, assessor e presidente do órgão julgador.");
        }
    }

    private void incorporarContextoNormativo(Processo processo, List<String> referencias, List<String> alertas) {
        try {
            NationalRulePackEngine.ResultadoRegras regras = rulePackEngine.aplicar(
                    new NationalRulePackEngine.ContextoRegra(
                            processo.getClasseProcessual(),
                            processo.getAssunto(),
                            processo.getRamoDireito(),
                            processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : GrauJurisdicao.PRIMEIRO_GRAU,
                            processo.getJurisdicao() != null ? processo.getJurisdicao().getCodigo() : "DESCONHECIDO",
                            Map.of("compliance", "impedimento_suspeicao")
                    )
            );
            referencias.addAll(regras.alertas());
            if (regras.bloqueante()) {
                alertas.add("Contexto normativo do processo já contém travas bloqueantes relevantes; trate o incidente de conflito com prioridade máxima.");
            }
        } catch (Exception e) {
            alertas.add("Contexto normativo indisponível para esta análise: " + safe(e.getMessage()));
        }
    }

    private MatrizConflito construirMatriz(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, List<String> alertas) {
        List<String> fatores = new ArrayList<>();
        List<String> provas = new ArrayList<>();
        List<String> travas = new ArrayList<>();

        double max = conflitos.stream().mapToDouble(ConflitoPotencial::gravidadeEstimada).max().orElse(0.0d);
        double media = conflitos.stream().mapToDouble(ConflitoPotencial::gravidadeEstimada).average().orElse(0.0d);
        int materialidade = scaleTo100((max * 0.65d) + (media * 0.35d));
        int probabilidade = scaleTo100((conflitos.size() / 6.0d) + (alertas.size() / 10.0d));
        int governanca = 30;

        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            governanca += 20;
            fatores.add("Autos sigilosos elevam a criticidade operacional do incidente.");
            travas.add("Fila restrita de análise e notificação mínima necessária.");
        }
        if (usuario.isMagistrado()) {
            governanca += 15;
            fatores.add("Agente com poder decisório direto exige governança reforçada.");
        }
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getGrau() != null
                && processo.getJurisdicao().getGrau().exigeColegiado()) {
            governanca += 10;
            fatores.add("Órgão colegiado demanda expansão da triagem para gabinete e presidência.");
        }
        if (conflitos.stream().anyMatch(ConflitoPotencial::bloqueante)) {
            governanca += 25;
            travas.add("Bloquear novos atos antes da definição formal do incidente.");
        }

        provas.add("CPF/OAB normalizados do agente e dos sujeitos processuais.");
        provas.add("Histórico de atuação no número unificado e em incidentes conexos.");
        provas.add("Declaração funcional do agente e validação de chefia/corregedoria.");
        if (conflitos.stream().anyMatch(c -> c.tipo() == TipoConflito.PARENTESCO_PARTE || c.tipo() == TipoConflito.ADVOGADO_MAGISTRADO_MESMO_ESCRITORIO)) {
            provas.add("Comprovação negativa ou positiva de parentesco/sociedade profissional.");
        }

        double gravidade = clamp((materialidade / 100.0d) * 0.50d + (probabilidade / 100.0d) * 0.20d + (governanca / 100.0d) * 0.30d);
        String classificacao = gravidade >= 0.90d ? "CRITICO"
                : gravidade >= 0.70d ? "ALTO"
                : gravidade >= 0.45d ? "MODERADO"
                : "BAIXO";

        return new MatrizConflito(materialidade, probabilidade, Math.min(governanca, 100), gravidade, classificacao, fatores, provas, travas);
    }

    private List<String> sugerirBlindagens(Usuario usuario, Processo processo, List<ConflitoPotencial> conflitos, MatrizConflito matriz) {
        List<String> blindagens = new ArrayList<>();
        blindagens.add("Capturar autodeclaração formal do agente antes de novos atos sensíveis.");
        blindagens.add("Registrar verificação em ledger imutável com hash canônico do incidente.");
        blindagens.add("Impedir redistribuição silenciosa sem trilha de responsável e motivo.");
        blindagens.add("Escalonar para validação humana quando a matriz atingir nível moderado ou superior.");
        blindagens.add("Comparar periodicamente CPF, OAB e vínculos territoriais com a base interna de usuários.");
        blindagens.add("Sincronizar o incidente com histórico operacional da UI para visibilidade controlada do workflow.");

        if (usuario.isMagistrado()) {
            blindagens.add("Expandir varredura para gabinete, assessor, revisor e presidência do órgão julgador.");
        }
        if (conflitos.stream().anyMatch(ConflitoPotencial::bloqueante) || matriz.classificacao().equals("CRITICO")) {
            blindagens.add("Bloquear prática de atos decisórios e expedições até resolução formal do incidente.");
        }
        if (processo.getRamoDireito() == RamoDireito.PENAL || processo.getRamoDireito() == RamoDireito.FAMILIA) {
            blindagens.add("Aplicar dupla checagem humana em audiências, cautelares e homologações sensíveis.");
        }
        return List.copyOf(blindagens);
    }

    private List<Usuario> inferirAgentesRelacionados(Processo processo) {
        LinkedHashSet<Usuario> agentes = new LinkedHashSet<>();
        if (processo.getUsuario() != null) {
            agentes.add(processo.getUsuario());
        }
        if (processo.getJurisdicao() != null && processo.getJurisdicao().getCidade() != null && !processo.getJurisdicao().getCidade().isBlank()) {
            usuarioRepository.findByComarcaAndAtivoTrue(processo.getJurisdicao().getCidade())
                    .stream()
                    .filter(Usuario::isAtivoESemanticoValido)
                    .filter(u -> u.isMagistrado() || u.isServidorJudiciario() || u.isPerito())
                    .limit(12)
                    .forEach(agentes::add);
        }
        return List.copyOf(agentes);
    }

    private void registrarAuditoria(Usuario usuarioAvaliado, Processo processo, ResultadoVerificacao resultado) {
        Usuario ator = currentUserService.getOrNull();
        String payloadHash = sha256Hex(String.join("|",
                String.valueOf(usuarioAvaliado.getId()),
                String.valueOf(processo.getId()),
                resultado.matriz().classificacao(),
                String.valueOf(resultado.matriz().gravidadeAgregada()),
                String.valueOf(resultado.conflitos().size())));
        String justificativa = "Análise nacional de impedimento/suspeição para processo " + safe(processo.getNumeroUnificado())
                + " e agente " + safe(usuarioAvaliado.getNome());
        auditLedger.appendSafely(
                "VERIFICACAO_CONFLITO_NACIONAL",
                RESOURCE_TYPE_PROCESSO,
                String.valueOf(processo.getId()),
                payloadHash,
                justificativa
        );
        if (ator != null) {
            auditLedger.appendSafely(
                    "VERIFICACAO_CONFLITO_OPERADOR",
                    RESOURCE_TYPE_COMPLIANCE,
                    String.valueOf(usuarioAvaliado.getId()),
                    payloadHash,
                    safe(ator.getNome())
            );
        }
    }

    private void publicarSinais(Usuario usuarioAvaliado, Processo processo, ResultadoVerificacao resultado) {
        registrarInbox(processo, resultado.acaoRecomendada(), resultado.temConflitoCritico(), resultado.exigeDeclaracao());
        eventPublisher.publishEvent(new ConflitoVerificadoEvent(
                processo.getId(),
                processo.getNumeroUnificado(),
                usuarioAvaliado.getId(),
                safe(usuarioAvaliado.getNome()),
                resultado.conflitosDetectados(),
                resultado.exigeAfastamento(),
                resultado.matriz().classificacao(),
                Instant.now()
        ));
    }

    private void registrarInbox(Processo processo, String mensagemBase, boolean critico, boolean exigeDeclaracao) {
        Usuario ator = currentUserService.getOrNull();
        EnumSet<UiToken> tokens = EnumSet.of(UiToken.INFO);
        if (critico) {
            tokens.add(UiToken.BLOQUEANTE);
            tokens.add(UiToken.URGENTE);
        }
        if (exigeDeclaracao) {
            tokens.add(UiToken.PENDENTE);
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            tokens.add(UiToken.SIGILOSO);
        }
        uiHistoryService.recordInboxEvent(
                "processo:" + processo.getId() + ":impedimento",
                processo.getId(),
                "PROCESSO_IMPEDIMENTO_SUSPEICAO",
                tokens,
                ator != null ? ator.getId() : null,
                ator != null && ator.getTipoUsuario() != null ? ator.getTipoUsuario().name() : null,
                safe(mensagemBase)
        );
    }

    private List<String> anexosLogicos(ResultadoVerificacao verificacao) {
        LinkedHashSet<String> anexos = new LinkedHashSet<>();
        anexos.add("MATRIZ_CONFLITO");
        anexos.add("CHECKLIST_GOVERNANCA");
        if (verificacao.temConflitoCritico()) {
            anexos.add("RELATORIO_CRITICIDADE");
        }
        if (verificacao.exigeDeclaracao()) {
            anexos.add("MINUTA_DECLARACAO");
        }
        return List.copyOf(anexos);
    }

    private FundamentoLegal resolverFundamento(TipoConflito tipo, RamoDireito ramo) {
        if (tipo == TipoConflito.IMPEDIMENTO_LEGAL || tipo == TipoConflito.IDENTIDADE_COM_PARTE || tipo == TipoConflito.ATUACAO_ANTERIOR_CAUSA) {
            if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
                return FundamentoLegal.CPP_ART_252_IMPEDIMENTO;
            }
            if (ramo == RamoDireito.TRABALHISTA) {
                return FundamentoLegal.CLT_ART_801_EXCECAO;
            }
            return FundamentoLegal.CPC_ART_144_IMPEDIMENTO;
        }
        if (tipo == TipoConflito.CONFLITO_INTERESSE_PERITO) {
            return FundamentoLegal.CPC_ART_148_AUXILIARES;
        }
        if (tipo == TipoConflito.ADVOGADO_MAGISTRADO_MESMO_ESCRITORIO || tipo == TipoConflito.IDENTIDADE_COM_PATRONO) {
            return FundamentoLegal.CODIGO_ETICA_OAB;
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            return FundamentoLegal.CPP_ART_254_SUSPEICAO;
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            return FundamentoLegal.CLT_ART_801_EXCECAO;
        }
        return FundamentoLegal.CPC_ART_145_SUSPEICAO;
    }

    private static boolean isPericiaSensivel(TipoUsuario tipoUsuario) {
        return tipoUsuario != null && (tipoUsuario.isPerito() || tipoUsuario == TipoUsuario.PERITO_MEDICO || tipoUsuario == TipoUsuario.PERITO_INSS || tipoUsuario == TipoUsuario.ASSISTENTE_TECNICO);
    }

    private static List<ConflitoPotencial> immutableDistinctConflitos(List<ConflitoPotencial> conflitos) {
        LinkedHashMap<String, ConflitoPotencial> unique = new LinkedHashMap<>();
        if (conflitos != null) {
            conflitos.stream().filter(Objects::nonNull).forEach(c -> unique.putIfAbsent(c.tipo().name() + "|" + safe(c.parteEnvolvida()) + "|" + safe(c.evidenciaChave()), c));
        }
        return List.copyOf(unique.values().stream().sorted(Comparator.comparingDouble(ConflitoPotencial::gravidadeEstimada).reversed()).toList());
    }

    private static List<String> immutableDistinct(List<String> values) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (values != null) {
            values.stream().filter(Objects::nonNull).map(ImpedimentoSuspeicaoEngine::safe).filter(v -> !v.isBlank()).forEach(set::add);
        }
        return List.copyOf(set);
    }

    private static int scaleTo100(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(Math.max(0.0d, value) * 100.0d)));
    }

    private static double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private static boolean sameNonBlank(String a, String b) {
        return !safe(a).isBlank() && safe(a).equalsIgnoreCase(safe(b));
    }

    private static String digitsOnly(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    private static boolean sharesSurname(String a, String b) {
        String la = lastToken(a);
        String lb = lastToken(b);
        return !la.isBlank() && la.equals(lb);
    }

    private static String lastToken(String value) {
        String normalized = normalizeKey(value);
        if (normalized.isBlank()) {
            return "";
        }
        String[] parts = normalized.split(" ");
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }

    private static String normalizeRole(String role) {
        return normalizeKey(role).replace(' ', '_');
    }

    private static String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(safe(value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(value));
        }
    }
}
