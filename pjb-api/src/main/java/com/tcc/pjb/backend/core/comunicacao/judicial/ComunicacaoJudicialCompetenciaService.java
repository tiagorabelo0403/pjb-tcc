package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;

@Service
public class ComunicacaoJudicialCompetenciaService {

    public enum AcaoComunicacaoJudicial {
        EXPEDIR,
        DEFLAGRAR_INTERCEPTACAO,
        REGISTRAR_FRUSTRACAO,
        CUMPRIR_DILIGENCIA_OFICIAL,
        PAINEL_OPERACIONAL
    }

    public record CompetenciaComunicacaoSnapshot(
            Long usuarioId,
            String usuarioNome,
            String perfil,
            AcaoComunicacaoJudicial acao,
            Long processoId,
            String processoNumero,
            String microssistema,
            String grauJurisdicao,
            String tribunalSuperior,
            String tipoComunicacao,
            String autoridadeCompetente,
            String executorPreferencial,
            boolean permitido,
            boolean requerMagistrado,
            boolean requerOficial,
            boolean reservaTribunal,
            boolean representaParte,
            boolean delegavelSecretaria,
            boolean revisaoRegimentalHumana,
            List<String> fundamentos,
            List<String> alertas
    ) {
    }

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final ExpedicaoJudicialRepository expedicaoRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;

    public ComunicacaoJudicialCompetenciaService(CurrentUserService currentUserService,
                                                 ProcessoRepository processoRepository,
                                                 ExpedicaoJudicialRepository expedicaoRepository,
                                                 LaianeProcuracaoRepository procuracaoRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.processoRepository = Objects.requireNonNull(processoRepository, "processoRepository");
        this.expedicaoRepository = Objects.requireNonNull(expedicaoRepository, "expedicaoRepository");
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository, "procuracaoRepository");
    }

    public CompetenciaComunicacaoSnapshot analisarExpedicao(Long processoId,
                                                            TipoComunicacaoJudicial tipoComunicacao,
                                                            boolean forcarOficial,
                                                            boolean forcarDigital) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = loadProcesso(processoId);
        return analisar(usuario, processo, tipoComunicacao, AcaoComunicacaoJudicial.EXPEDIR, forcarOficial, forcarDigital, null);
    }

    public CompetenciaComunicacaoSnapshot analisarInterceptacao(Long processoId,
                                                                TipoComunicacaoJudicial tipoComunicacao,
                                                                boolean forcarOficial) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = loadProcesso(processoId);
        return analisar(usuario, processo, tipoComunicacao, AcaoComunicacaoJudicial.DEFLAGRAR_INTERCEPTACAO, forcarOficial, true, null);
    }

    public CompetenciaComunicacaoSnapshot analisarFrustracao(String expedicaoUuid) {
        Usuario usuario = currentUserService.getRequired();
        ExpedicaoJudicial expedicao = loadExpedicao(expedicaoUuid);
        Processo processo = loadProcesso(expedicao.getProcessoId());
        return analisar(usuario, processo, expedicao.getTipoComunicacao(), AcaoComunicacaoJudicial.REGISTRAR_FRUSTRACAO,
                expedicao.getModalidade() != null && expedicao.getModalidade().isExigeOficial(),
                expedicao.getModalidade() != null && expedicao.getModalidade().isDigital(),
                expedicao);
    }

    public CompetenciaComunicacaoSnapshot analisarPainel(Long processoId) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = processoId != null ? loadProcesso(processoId) : null;
        return analisar(usuario, processo, null, AcaoComunicacaoJudicial.PAINEL_OPERACIONAL, false, false, null);
    }

    public void exigirExpedicao(Long processoId,
                                TipoComunicacaoJudicial tipoComunicacao,
                                boolean forcarOficial,
                                boolean forcarDigital) {
        exigir(analisarExpedicao(processoId, tipoComunicacao, forcarOficial, forcarDigital));
    }

    public void exigirInterceptacao(Long processoId,
                                    TipoComunicacaoJudicial tipoComunicacao,
                                    boolean forcarOficial) {
        exigir(analisarInterceptacao(processoId, tipoComunicacao, forcarOficial));
    }

    public void exigirFrustracao(String expedicaoUuid) {
        exigir(analisarFrustracao(expedicaoUuid));
    }

    public void exigirPainel(Long processoId) {
        exigir(analisarPainel(processoId));
    }

    private void exigir(CompetenciaComunicacaoSnapshot snapshot) {
        if (snapshot != null && snapshot.permitido()) {
            return;
        }
        String message = snapshot == null
                ? "Competência comunicacional indisponível"
                : firstNonBlank(
                        snapshot.alertas().isEmpty() ? null : snapshot.alertas().getFirst(),
                        snapshot.fundamentos().isEmpty() ? null : snapshot.fundamentos().getFirst(),
                        "Ato reservado à autoridade judicial competente"
                );
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private CompetenciaComunicacaoSnapshot analisar(Usuario usuario,
                                                    Processo processo,
                                                    TipoComunicacaoJudicial tipoComunicacao,
                                                    AcaoComunicacaoJudicial acao,
                                                    boolean forcarOficial,
                                                    boolean forcarDigital,
                                                    ExpedicaoJudicial expedicao) {
        List<String> fundamentos = new ArrayList<>();
        List<String> alertas = new ArrayList<>();
        TipoUsuario perfil = usuario != null ? usuario.getTipoUsuario() : null;
        boolean magistrado = usuario != null && usuario.isMagistrado();
        boolean servidor = usuario != null && (usuario.isServidorJudiciario() || perfil != null && perfil.isAssessor());
        boolean oficial = perfil == TipoUsuario.OFICIAL_JUSTICA || perfil == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
        boolean representante = usuario != null
                && usuario.isAdvogado()
                && processo != null
                && usuario.getId() != null
                && procuracaoRepository.existsByAdvogado_IdAndProcessoIdAndStatus(usuario.getId(), processo.getId(), LaianeProcuracaoStatus.ATIVA);
        boolean processoAtivo = processo == null || processo.getStatusProcesso() == null || processo.getStatusProcesso().isAtivo();
        ProceduralCommunicationContext context = tipoComunicacao != null ? ProceduralCommunicationContext.from(processo, tipoComunicacao, null) : null;
        ComunicacaoJudicialMicrossistema microssistema = context != null
                ? context.microssistema()
                : ComunicacaoJudicialMicrossistemaResolver.resolver(processo);
        GrauJurisdicao grau = context != null ? context.grau() : processo != null && processo.getJurisdicao() != null ? processo.getJurisdicao().getGrau() : null;
        boolean atoCitacao = tipoComunicacao != null && tipoComunicacao.isCitacao();
        boolean atoEdital = tipoComunicacao != null && tipoComunicacao.isEdital();
        boolean atoMandado = tipoComunicacao != null && tipoComunicacao.isMandado();
        boolean atoPessoal = tipoComunicacao != null && tipoComunicacao.isExigePessoalidade();
        boolean atoInternacional = tipoComunicacao == TipoComunicacaoJudicial.CARTA_ROGATORIA_EXPEDIDA || context != null && context.isInternacionalOuCooperacao();
        boolean faseRecursalEstrita = context != null && (context.isRecursal()
                || context.isEmbargosDeclaracao()
                || context.isEmbargosExecucao()
                || context.isEmbargosTerceiro()
                || context.isEmbargosDivergencia());
        boolean microssistemaSensivel = isMicrossistemaSensivel(microssistema, context);
        boolean trabalhistaInicial = processo != null
                && processo.getRamoDireito() == RamoDireito.TRABALHISTA
                && tipoComunicacao == TipoComunicacaoJudicial.CITACAO_INICIAL;
        boolean juizadoComunicacaoOrdinaria = context != null && context.isJuizadoOuRitoSimplificado()
                && tipoComunicacao != null
                && (tipoComunicacao.isIntimacao() || tipoComunicacao.isCitacao());
        boolean reservaTribunal = grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL || context != null && context.isTribunalSuperiorOuConstitucional();
        ComunicacaoJudicialTribunalSuperior tribunalSuperior = context != null ? context.tribunalSuperior() : ComunicacaoJudicialTribunalSuperior.NENHUM;
        boolean requerOficial = expedicao != null
                ? expedicao.getModalidade() != null && expedicao.getModalidade().isExigeOficial()
                : forcarOficial
                || atoPessoal
                || context != null && (context.isPenalSensivel() || context.isExecucaoPenal() || context.isFamiliaOuInfanciaSensivel() && atoCitacao);
        boolean reservadoMagistrado = atoEdital
                || atoInternacional
                || atoCitacao && microssistemaSensivel
                || atoMandado && microssistemaSensivel
                || faseRecursalEstrita && (atoCitacao || reservaTribunal || microssistemaSensivel)
                || reservaTribunal && (atoCitacao || atoEdital || faseRecursalEstrita)
                || context != null && context.isConstitucionalOriginario()
                || forcarOficial && microssistemaSensivel;
        boolean delegavelSecretaria = tipoComunicacao != null
                && !reservadoMagistrado
                && !representante
                && !oficial
                && !reservaTribunal
                && !(context != null && context.exigeRevisaoRegimentalHumana())
                && (
                trabalhistaInicial
                        || juizadoComunicacaoOrdinaria
                        || tipoComunicacao.isIntimacao()
                        || tipoComunicacao.getNatureza() == TipoComunicacaoJudicial.NaturezaAto.NOTIFICACAO
                        || tipoComunicacao == TipoComunicacaoJudicial.CARTA_PRECATORIA_EXPEDIDA
                        || tipoComunicacao == TipoComunicacaoJudicial.COMUNICACAO_COOPERACAO_NACIONAL
        );
        boolean revisaoRegimentalHumana = context != null && context.exigeRevisaoRegimentalHumana();
        ComunicacaoJudicialAutoridadeCompetente autoridadeCompetente = resolverAutoridadeCompetente(context, acao, reservadoMagistrado, delegavelSecretaria, representante, requerOficial, reservaTribunal, atoInternacional, tribunalSuperior);
        ComunicacaoJudicialAutoridadeCompetente executorPreferencial = resolverExecutorPreferencial(acao, autoridadeCompetente, requerOficial, representante, reservaTribunal, atoInternacional);

        fundamentos.add("CF art. 5º, LIII, LIV e LV: autoridade competente, devido processo legal, contraditório e ampla defesa.");
        if (acao == AcaoComunicacaoJudicial.EXPEDIR || acao == AcaoComunicacaoJudicial.PAINEL_OPERACIONAL) {
            fundamentos.add("CPC arts. 152, 154, 246 e 270: secretaria pratica atos ordinatórios; citação e intimação observam forma legal, pessoalidade e canal admitido.");
        }
        if (processo != null && processo.getRamoDireito() == RamoDireito.TRABALHISTA) {
            fundamentos.add("CLT art. 841: notificação inicial trabalhista é expedida pela secretaria, ordinariamente por via oficial do juízo.");
        }
        if (processo != null && processo.getRamoDireito() == RamoDireito.PENAL) {
            fundamentos.add("CPP arts. 351 e 370: citação penal e intimações seguem regime próprio, com reforço de autenticidade e pessoalidade.");
        }
        if (processo != null && processo.getRito() != null && processo.getRito().name().startsWith("JUIZADO_ESPECIAL")) {
            fundamentos.add("Lei 9.099/1995 arts. 18 e 19: citações e intimações nos Juizados observam meio idôneo, celeridade e simplicidade sem dispensar autenticidade.");
        }
        if (context != null && context.rito() != null && context.rito().isEleitoral()) {
            fundamentos.add("Código Eleitoral e legislação especial eleitoral: prazos reduzidos e reserva técnica maior em feitos eleitorais sensíveis e recursais.");
        }
        if (context != null && context.rito() != null && context.rito().isMilitar()) {
            fundamentos.add("CPPM e Justiça Militar: comunicação pessoal e reserva funcional reforçadas em atos de maior gravidade, instrução penal militar e conselho de justiça.");
        }
        if (forcarDigital || tipoComunicacao != null && tipoComunicacao.isAdmiteDigital()) {
            fundamentos.add("Lei 11.419/2006 e Resolução CNJ 455/2022: comunicações eletrônicas exigem canal oficial, autenticação, trilha de ciência e aderência ao Domicílio Judicial Eletrônico.");
        }
        if (reservaTribunal) {
            fundamentos.add("Atos em segundo grau qualificado, tribunais superiores ou controle constitucional exigem cautela ampliada de admissibilidade, ciência e competência funcional.");
        }

        boolean permitido;
        switch (acao) {
            case PAINEL_OPERACIONAL -> permitido = magistrado || servidor || oficial;
            case CUMPRIR_DILIGENCIA_OFICIAL -> permitido = oficial && expedicaoCompativelComOficial(expedicao, usuario);
            case REGISTRAR_FRUSTRACAO -> permitido = avaliarPermissaoFrustracao(magistrado, servidor, oficial, reservadoMagistrado, delegavelSecretaria, expedicao, usuario, alertas);
            case DEFLAGRAR_INTERCEPTACAO -> permitido = avaliarPermissaoInterceptacao(magistrado, servidor, oficial, reservadoMagistrado, delegavelSecretaria, tipoComunicacao, requerOficial, processoAtivo, alertas);
            case EXPEDIR -> permitido = avaliarPermissaoExpedicao(magistrado, servidor, oficial, representante, delegavelSecretaria, reservadoMagistrado, processoAtivo, tipoComunicacao, alertas);
            default -> permitido = false;
        }

        if (!processoAtivo) {
            permitido = false;
            alertas.add("Processo encerrado não admite nova expedição ou interceptação sem determinação judicial superveniente.");
        }
        if (representante) {
            alertas.add("Advogado habilitado recebe a comunicação e o prazo como representante, mas não pratica o ato oficial de citar ou intimar em nome do juízo.");
        }
        if (oficial && (acao == AcaoComunicacaoJudicial.EXPEDIR || acao == AcaoComunicacaoJudicial.DEFLAGRAR_INTERCEPTACAO)) {
            alertas.add("Oficial de justiça cumpre diligência e certifica o ato; a expedição continua vinculada à ordem judicial ou à secretaria competente.");
        }
        if (reservadoMagistrado && !magistrado) {
            alertas.add("Este cenário foi classificado como reservado à magistratura por envolver edital, cooperação internacional, microssistema sensível, grau recursal qualificado ou citação pessoal restritiva.");
        }
        if (reservaTribunal) {
            alertas.add("Feito em grau de jurisdição qualificado exige validação funcional reforçada e reduz hipóteses de delegação material.");
        }
        if (context != null && context.isEmbargosDivergencia()) {
            alertas.add("Embargos de divergência foram sinalizados como movimento de tribunal superior com tratamento institucional mais restrito.");
        }
        if (revisaoRegimentalHumana) {
            alertas.add("Incidente raro ou classe superior exige conferência regimental humana antes da automação final de prazo, competência e forma de ciência.");
        }

        return new CompetenciaComunicacaoSnapshot(
                usuario != null ? usuario.getId() : null,
                usuario != null ? usuario.getNome() : null,
                perfil != null ? perfil.name() : null,
                acao,
                processo != null ? processo.getId() : null,
                processo != null ? processo.getNumeroProcesso() : null,
                microssistema.name(),
                grau != null ? grau.name() : null,
                tribunalSuperior.name(),
                tipoComunicacao != null ? tipoComunicacao.name() : null,
                autoridadeCompetente.name(),
                executorPreferencial.name(),
                permitido,
                reservadoMagistrado,
                requerOficial,
                reservaTribunal,
                representante,
                delegavelSecretaria,
                revisaoRegimentalHumana,
                List.copyOf(distinctNonBlank(fundamentos)),
                List.copyOf(distinctNonBlank(alertas))
        );
    }

    private boolean avaliarPermissaoExpedicao(boolean magistrado,
                                              boolean servidor,
                                              boolean oficial,
                                              boolean representante,
                                              boolean delegavelSecretaria,
                                              boolean reservadoMagistrado,
                                              boolean processoAtivo,
                                              TipoComunicacaoJudicial tipoComunicacao,
                                              List<String> alertas) {
        if (magistrado) {
            return true;
        }
        if (!processoAtivo || oficial || representante) {
            return false;
        }
        if (servidor && delegavelSecretaria) {
            return true;
        }
        if (servidor && !reservadoMagistrado && tipoComunicacao != null && tipoComunicacao.isAdmiteDigital() && tipoComunicacao.isIntimacao()) {
            return true;
        }
        alertas.add("Expedição negada para o perfil atual: o ato exige ordem judicial direta ou atuação restrita da secretaria em hipótese normativa delimitada.");
        return false;
    }

    private boolean avaliarPermissaoInterceptacao(boolean magistrado,
                                                  boolean servidor,
                                                  boolean oficial,
                                                  boolean reservadoMagistrado,
                                                  boolean delegavelSecretaria,
                                                  TipoComunicacaoJudicial tipoComunicacao,
                                                  boolean requerOficial,
                                                  boolean processoAtivo,
                                                  List<String> alertas) {
        if (!processoAtivo || oficial) {
            return false;
        }
        if (magistrado) {
            return true;
        }
        if (servidor && delegavelSecretaria && !reservadoMagistrado && !requerOficial && (tipoComunicacao == null || tipoComunicacao.isAdmiteDigital())) {
            return true;
        }
        alertas.add("Interceptação ativa é ferramenta institucional do juízo e da secretaria, vedada quando o ato exigir oficial, reserva de magistratura ou canal não oficial.");
        return false;
    }

    private boolean avaliarPermissaoFrustracao(boolean magistrado,
                                               boolean servidor,
                                               boolean oficial,
                                               boolean reservadoMagistrado,
                                               boolean delegavelSecretaria,
                                               ExpedicaoJudicial expedicao,
                                               Usuario usuario,
                                               List<String> alertas) {
        if (magistrado) {
            return true;
        }
        if (oficial) {
            boolean permitido = expedicaoCompativelComOficial(expedicao, usuario);
            if (!permitido) {
                alertas.add("Frustração por oficial exige mandado compatível com a diligência física atribuída.");
            }
            return permitido;
        }
        if (servidor && !reservadoMagistrado && delegavelSecretaria) {
            return true;
        }
        alertas.add("Registro de frustração exige secretaria do juízo ou oficial responsável, preservada a reserva em casos sensíveis ou de grau qualificado.");
        return false;
    }

    private boolean expedicaoCompativelComOficial(ExpedicaoJudicial expedicao, Usuario usuario) {
        if (expedicao == null || usuario == null || usuario.getId() == null) {
            return false;
        }
        boolean oficialRoute = expedicao.getModalidade() != null && expedicao.getModalidade().isExigeOficial();
        boolean pendenteOficial = expedicao.getStatus() == ExpedicaoJudicial.StatusExpedicao.PENDENTE_OFICIAL || oficialRoute;
        boolean sameOfficial = expedicao.getOficialId() == null || Objects.equals(expedicao.getOficialId(), usuario.getId());
        return pendenteOficial && sameOfficial;
    }

    private boolean isMicrossistemaSensivel(ComunicacaoJudicialMicrossistema microssistema,
                                            ProceduralCommunicationContext context) {
        if (microssistema == null) {
            return false;
        }
        return switch (microssistema) {
            case CONSTITUCIONAL_ORIGINARIO,
                 CONSTITUCIONAL_RECURSAL,
                 TRIBUNAIS_SUPERIORES_CIVEIS,
                 TRIBUNAL_SUPERIOR_TRABALHISTA,
                 TRIBUNAL_SUPERIOR_ELEITORAL,
                 TRIBUNAL_SUPERIOR_MILITAR,
                 INTERNACIONAL_COOPERACAO,
                 MILITAR_ESPECIAL,
                 ELEITORAL_ESPECIAL,
                 EXECUCAO_PENAL,
                 INFANCIA_JUVENTUDE -> true;
            case PENAL_COMUM,
                 PENAL_ESPECIAL -> context != null && context.isPenalSensivel();
            default -> false;
        };
    }

    private ComunicacaoJudicialAutoridadeCompetente resolverAutoridadeCompetente(ProceduralCommunicationContext context,
                                                                                  AcaoComunicacaoJudicial acao,
                                                                                  boolean reservadoMagistrado,
                                                                                  boolean delegavelSecretaria,
                                                                                  boolean representante,
                                                                                  boolean requerOficial,
                                                                                  boolean reservaTribunal,
                                                                                  boolean atoInternacional,
                                                                                  ComunicacaoJudicialTribunalSuperior tribunalSuperior) {
        if (representante) {
            return ComunicacaoJudicialAutoridadeCompetente.REPRESENTACAO_PROCESSUAL;
        }
        if (atoInternacional) {
            return ComunicacaoJudicialAutoridadeCompetente.COOPERACAO_INTERNACIONAL;
        }
        if (requerOficial && acao == AcaoComunicacaoJudicial.CUMPRIR_DILIGENCIA_OFICIAL) {
            return ComunicacaoJudicialAutoridadeCompetente.OFICIAL_JUSTICA;
        }
        if (context != null && context.isSuspensaoSegurancaOuLiminar()) {
            return ComunicacaoJudicialAutoridadeCompetente.PRESIDENCIA_TRIBUNAL;
        }
        if (reservaTribunal || tribunalSuperior.isSuperior()) {
            if (context != null && (context.isEmbargosDivergencia() || context.isIncidenteRepetitivoOuAssuncao())) {
                return ComunicacaoJudicialAutoridadeCompetente.ORGAO_COLEGIADO_TRIBUNAL;
            }
            if (context != null && context.exigeRevisaoRegimentalHumana()) {
                return ComunicacaoJudicialAutoridadeCompetente.RELATOR_TRIBUNAL;
            }
            return ComunicacaoJudicialAutoridadeCompetente.APOIO_TRIBUNAL;
        }
        if (reservadoMagistrado) {
            return ComunicacaoJudicialAutoridadeCompetente.MAGISTRADO;
        }
        if (delegavelSecretaria) {
            return ComunicacaoJudicialAutoridadeCompetente.SECRETARIA_JUDICIAL;
        }
        if (requerOficial) {
            return ComunicacaoJudicialAutoridadeCompetente.OFICIAL_JUSTICA;
        }
        return ComunicacaoJudicialAutoridadeCompetente.MAGISTRADO;
    }

    private ComunicacaoJudicialAutoridadeCompetente resolverExecutorPreferencial(AcaoComunicacaoJudicial acao,
                                                                                  ComunicacaoJudicialAutoridadeCompetente autoridadeCompetente,
                                                                                  boolean requerOficial,
                                                                                  boolean representante,
                                                                                  boolean reservaTribunal,
                                                                                  boolean atoInternacional) {
        if (representante) {
            return ComunicacaoJudicialAutoridadeCompetente.REPRESENTACAO_PROCESSUAL;
        }
        if (atoInternacional) {
            return ComunicacaoJudicialAutoridadeCompetente.COOPERACAO_INTERNACIONAL;
        }
        if (acao == AcaoComunicacaoJudicial.CUMPRIR_DILIGENCIA_OFICIAL || requerOficial) {
            return ComunicacaoJudicialAutoridadeCompetente.OFICIAL_JUSTICA;
        }
        if (reservaTribunal || autoridadeCompetente.isTribunal()) {
            return autoridadeCompetente;
        }
        return autoridadeCompetente == ComunicacaoJudicialAutoridadeCompetente.MAGISTRADO
                ? ComunicacaoJudicialAutoridadeCompetente.SECRETARIA_JUDICIAL
                : autoridadeCompetente;
    }

    private Processo loadProcesso(Long processoId) {
        if (processoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "processoId é obrigatório para avaliar competência comunicacional");
        }
        return processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processo não encontrado: " + processoId));
    }

    private ExpedicaoJudicial loadExpedicao(String expedicaoUuid) {
        return expedicaoRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expedição não encontrada: " + expedicaoUuid));
    }

    private static List<String> distinctNonBlank(List<String> values) {
        List<String> out = new ArrayList<>();
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null && out.stream().noneMatch(normalized::equals)) {
                out.add(normalized);
            }
        }
        return out;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
