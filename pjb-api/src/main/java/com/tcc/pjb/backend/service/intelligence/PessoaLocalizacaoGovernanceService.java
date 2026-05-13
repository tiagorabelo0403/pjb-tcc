package com.tcc.pjb.backend.service.intelligence;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.tcc.pjb.backend.core.security.device.DeviceRiskEngine;
import com.tcc.pjb.backend.core.security.device.RiskDecision;
import com.tcc.pjb.backend.core.security.device.RiskEvaluation;
import com.tcc.pjb.backend.core.security.stepup.JwtStepUpClaims;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoConsultaResumo;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoRequest;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoCanalConsulta;
import com.tcc.pjb.backend.model.entity.intelligence.PessoaLocalizacaoConsultaGovernada;
import com.tcc.pjb.backend.model.repository.intelligence.PessoaLocalizacaoConsultaGovernadaRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.service.profile.PerfilBehavioralAuditService;

@Service
public class PessoaLocalizacaoGovernanceService {

    public record PosturaConsulta(
            String level,
            int score,
            boolean requiresReview,
            boolean offHours,
            String releaseMode,
            boolean stepUpRequired,
            boolean stepUpSatisfied,
            String challengeHint,
            List<String> sinais
    ) {
        public PosturaConsulta {
            sinais = sinais == null ? List.of() : List.copyOf(sinais);
        }
    }

    private final PessoaLocalizacaoConsultaGovernadaRepository repository;
    private final PerfilBehavioralAuditService behavioralAuditService;
    private final UserSecurityProfileRepository userSecurityProfileRepository;
    private final DeviceRiskEngine deviceRiskEngine;
    private final PjbTimeService timeService;

    public PessoaLocalizacaoGovernanceService(PessoaLocalizacaoConsultaGovernadaRepository repository,
                                              PerfilBehavioralAuditService behavioralAuditService,
                                              UserSecurityProfileRepository userSecurityProfileRepository,
                                              DeviceRiskEngine deviceRiskEngine,
                                              PjbTimeService timeService) {
        this.repository = Objects.requireNonNull(repository);
        this.behavioralAuditService = Objects.requireNonNull(behavioralAuditService);
        this.userSecurityProfileRepository = Objects.requireNonNull(userSecurityProfileRepository);
        this.deviceRiskEngine = Objects.requireNonNull(deviceRiskEngine);
        this.timeService = Objects.requireNonNull(timeService);
    }

    @Transactional(readOnly = true)
    public PosturaConsulta avaliar(Usuario executor,
                                   PessoaLocalizacaoService.CanalConsulta canal,
                                   PessoaLocalizacaoRequest request,
                                   boolean possuiContextoFormal,
                                   boolean enderecoEstritoLiberado,
                                   boolean enderecoEstritoElegivel) {
        PerfilDashboardPayload.BehavioralAuditResumo behavioral = behavioralAuditService.avaliar(executor);
        var profile = executor != null && executor.getId() != null ? userSecurityProfileRepository.findByUserId(executor.getId()).orElse(null) : null;
        RiskEvaluation sessionRisk = deviceRiskEngine.evaluateFirstLink(executor, resolveClientIp(), profile, false);
        LocalDateTime now = LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone());
        boolean offHours = now.getHour() < 6 || now.getHour() >= 21;

        int score = 0;
        LinkedHashSet<String> sinais = new LinkedHashSet<>();

        if (!possuiContextoFormal) {
            score += 24;
            sinais.add("SEM_CONTEXTO_FORMAL");
        }
        if (request.exigirEnderecoEstrito()) {
            score += 18;
            sinais.add("ENDERECO_ESTRITO_SOLICITADO");
        }
        if (enderecoEstritoLiberado) {
            score += 12;
            sinais.add("ENDERECO_ESTRITO_LIBERADO");
        }
        if (!enderecoEstritoElegivel) {
            sinais.add("ENDERECO_MINIMIZADO_POR_POLITICA");
        }
        if (behavioral.anomalous()) {
            score += 26;
            sinais.add("COMPORTAMENTO_ANOMALO");
        } else if ("ALERTA".equalsIgnoreCase(behavioral.level())) {
            score += 12;
            sinais.add("COMPORTAMENTO_EM_ALERTA");
        }
        if (sessionRisk.decision() == RiskDecision.CHALLENGE) {
            score += 18;
            sinais.add("RISCO_SESSAO_ELEVADO");
        } else if (sessionRisk.decision() == RiskDecision.DENY) {
            score += 35;
            sinais.add("RISCO_SESSAO_CRITICO");
        } else if (sessionRisk.riskScore() >= 60) {
            score += 10;
            sinais.add("RISCO_SESSAO_MODERADO");
        }
        if (executor != null && executor.getTipoUsuario() != null && executor.getTipoUsuario().isPerfilCritico()) {
            score += 10;
            sinais.add("PERFIL_CRITICO");
        }
        if (offHours) {
            score += 8;
            sinais.add("FORA_JANELA_OPERACIONAL_PADRAO");
        }
        if (request.incluirMandados()) {
            score += 4;
            sinais.add("CORRELACAO_MANDADOS");
        }
        if (request.incluirRestricoes()) {
            score += 4;
            sinais.add("CORRELACAO_RESTRICOES");
        }
        if (request.incluirProntuario()) {
            score += 3;
            sinais.add("CORRELACAO_PRONTUARIO");
        }
        if (canal == PessoaLocalizacaoService.CanalConsulta.MAGISTRATURA) {
            sinais.add("CANAL_GABINETE_OU_MAGISTRATURA");
        }
        boolean stepUpRequired = enderecoEstritoLiberado
                && (request.exigirEnderecoEstrito() || !possuiContextoFormal || sessionRisk.decision() == RiskDecision.CHALLENGE || sessionRisk.decision() == RiskDecision.DENY || behavioral.anomalous());
        boolean stepUpSatisfied = !stepUpRequired || JwtStepUpClaims.hasMfa();
        if (stepUpRequired) {
            sinais.add(stepUpSatisfied ? "STEP_UP_SATISFEITO" : "STEP_UP_PENDENTE");
            score = Math.max(score, stepUpSatisfied ? 42 : 62);
        }
        String level = score >= 75 ? "CRITICO" : score >= 55 ? "ALTO" : score >= 30 ? "MEDIO" : "BAIXO";
        String challengeHint = stepUpRequired && !stepUpSatisfied ? "/api/v1/auth/stepup/start" : null;
        boolean requiresReview = score >= 55 || (!possuiContextoFormal && request.exigirEnderecoEstrito()) || stepUpRequired;
        String releaseMode = resolveReleaseMode(executor, possuiContextoFormal, request.exigirEnderecoEstrito(), enderecoEstritoLiberado, stepUpRequired, stepUpSatisfied);
        return new PosturaConsulta(level, score, requiresReview, offHours, releaseMode, stepUpRequired, stepUpSatisfied, challengeHint, List.copyOf(sinais));
    }

    @Transactional
    public void registrar(String correlationId,
                          Usuario executor,
                          String cpf,
                          PessoaLocalizacaoService.CanalConsulta canal,
                          PessoaLocalizacaoRequest request,
                          PessoaLocalizacaoResponse response,
                          PosturaConsulta postura) {
        if (executor == null || executor.getId() == null || response == null || postura == null) {
            return;
        }
        PessoaLocalizacaoConsultaGovernada entity = PessoaLocalizacaoConsultaGovernada.builder()
                .correlationId(correlationId)
                .executorUserId(executor.getId())
                .executorTipoUsuario(executor.getTipoUsuario())
                .canalConsulta(toEntityCanal(canal))
                .fundamento(resolveFundamentoSeguro(request, response, canal))
                .processoId(request.processoId())
                .mandadoId(request.mandadoId())
                .referenciaProcedimental(normalizeText(firstNonBlank(
                        response.governanca() != null ? response.governanca().referenciaProcedimental() : null,
                        resolveReferenciaProcedimental(request)
                ), 160, "SEM_REFERENCIA_FORMAL"))
                .finalidade(normalizeText(response.finalidade(), 500, "FINALIDADE_NAO_INFORMADA"))
                .justificativaOperacional(normalizeText(request.justificativaOperacional(), 1000, "NAO INFORMADA"))
                .cpfHash(CriptografiaPJB.hashCpfCnpj(cpf))
                .cpfMascarado(normalizeText(response.cpfMascarado(), 32, "***"))
                .possuiContextoFormal(response.governanca() != null && response.governanca().possuiContextoFormal())
                .consultaSemProcessoAutorizada(response.governanca() != null && response.governanca().consultaSemProcessoAutorizada())
                .enderecoEstritoSolicitado(request.exigirEnderecoEstrito())
                .enderecoEstritoLiberado(response.enderecoEstritoLiberado())
                .nivelExposicao(normalizeText(response.nivelExposicao(), 32, "MINIMIZADO"))
                .posturaNivel(normalizeText(postura.level(), 24, "BAIXO"))
                .posturaScore(postura.score())
                .requerRevisao(postura.requiresReview())
                .modoLiberacao(normalizeText(postura.releaseMode(), 60, "FORMAL_MINIMIZADO"))
                .stepUpRequired(postura.stepUpRequired())
                .stepUpSatisfied(postura.stepUpSatisfied())
                .challengeHint(normalizeNullableText(postura.challengeHint(), 180))
                .fontesConsultadas(response.fontes() == null ? 0 : response.fontes().size())
                .enderecosEncontrados(response.enderecos() == null ? 0 : response.enderecos().size())
                .restricoesEncontradas(response.restricoes() == null ? 0 : response.restricoes().size())
                .vinculosEncontrados(response.vinculosProcessuais() == null ? 0 : response.vinculosProcessuais().size())
                .alertasCount(response.alertas() == null ? 0 : response.alertas().size())
                .sinaisPostura(normalizeText(String.join("|", postura.sinais()), 1500, "SEM_SINAIS"))
                .createdAt(LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone()))
                .build();
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<PessoaLocalizacaoConsultaResumo> listarRecentes(Usuario executor,
                                                                PessoaLocalizacaoService.CanalConsulta canal,
                                                                int limit) {
        if (executor == null || executor.getId() == null || canal == null) {
            return List.of();
        }
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return repository.findByExecutorUserIdAndCanalConsultaOrderByCreatedAtDesc(executor.getId(), toEntityCanal(canal), PageRequest.of(0, safeLimit)).stream()
                .map(entity -> new PessoaLocalizacaoConsultaResumo(
                        entity.getCorrelationId(),
                        entity.getCreatedAt() == null ? null : entity.getCreatedAt().atZone(timeService.legalZone()).toInstant(),
                        fromEntityCanal(entity.getCanalConsulta()),
                        entity.getFundamento(),
                        entity.getReferenciaProcedimental(),
                        entity.getFinalidade(),
                        entity.getNivelExposicao(),
                        entity.getPosturaNivel(),
                        entity.getPosturaScore(),
                        entity.isRequerRevisao(),
                        entity.isPossuiContextoFormal(),
                        entity.isEnderecoEstritoLiberado(),
                        entity.getFontesConsultadas(),
                        entity.getEnderecosEncontrados(),
                        entity.getRestricoesEncontradas(),
                        entity.getVinculosEncontrados()
                ))
                .toList();
    }

    private static PessoaLocalizacaoCanalConsulta toEntityCanal(PessoaLocalizacaoService.CanalConsulta canal) {
        return canal == null ? null : PessoaLocalizacaoCanalConsulta.valueOf(canal.name());
    }

    private static PessoaLocalizacaoService.CanalConsulta fromEntityCanal(PessoaLocalizacaoCanalConsulta canal) {
        return canal == null ? null : PessoaLocalizacaoService.CanalConsulta.valueOf(canal.name());
    }

    private static com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento resolveFundamentoSeguro(PessoaLocalizacaoRequest request,
                                                                                                                    PessoaLocalizacaoResponse response,
                                                                                                                    PessoaLocalizacaoService.CanalConsulta canal) {
        if (request != null && request.fundamento() != null) {
            return request.fundamento();
        }
        if (response != null && response.governanca() != null && response.governanca().fundamento() != null && !response.governanca().fundamento().isBlank()) {
            try {
                return com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento.valueOf(response.governanca().fundamento().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return switch (canal) {
            case OFICIAL_JUSTICA -> com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento.CUMPRIMENTO_MANDADO;
            case DELEGADO -> com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento.INVESTIGACAO_POLICIAL_FORMAL;
            case MAGISTRATURA -> com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoFundamento.DECISAO_JUDICIAL_EXECUTIVA;
        };
    }

    private static String normalizeText(String value, int max, String fallback) {
        String normalized = normalizeNullableText(value, max);
        return normalized == null ? fallback : normalized;
    }

    private static String normalizeNullableText(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private static String resolveReleaseMode(Usuario executor,
                                             boolean possuiContextoFormal,
                                             boolean enderecoEstritoSolicitado,
                                             boolean enderecoEstritoLiberado,
                                             boolean stepUpRequired,
                                             boolean stepUpSatisfied) {
        if (enderecoEstritoLiberado && stepUpRequired && !stepUpSatisfied) {
            return "STEPUP_PENDENTE_MINIMIZADO";
        }
        if (enderecoEstritoLiberado) {
            if (executor != null && executor.getTipoUsuario() != null && executor.getTipoUsuario().isAssessor()) {
                return "DELEGADO_ESTRITO_CONTROLADO";
            }
            return possuiContextoFormal ? "FORMAL_ESTRITO" : "ABERTO_ESTRITO_CONTROLADO";
        }
        if (enderecoEstritoSolicitado) {
            return "SOLICITADO_MINIMIZADO";
        }
        return possuiContextoFormal ? "FORMAL_MINIMIZADO" : "ABERTO_MINIMIZADO";
    }

    private static String resolveReferenciaProcedimental(PessoaLocalizacaoRequest request) {
        if (request.referenciaProcedimental() != null && !request.referenciaProcedimental().isBlank()) {
            return request.referenciaProcedimental().trim();
        }
        if (request.processoId() != null) {
            return "PROC-" + request.processoId();
        }
        if (request.mandadoId() != null) {
            return "MAND-" + request.mandadoId();
        }
        return "SEM_REFERENCIA_FORMAL";
    }


    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return InetAddress.getLoopbackAddress().getHostAddress();
            }
            var request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String real = request.getHeader("X-Real-IP");
            if (real != null && !real.isBlank()) {
                return real.trim();
            }
            String remote = request.getRemoteAddr();
            if (remote != null && !remote.isBlank()) {
                return remote.trim();
            }
            return InetAddress.getLoopbackAddress().getHostAddress();
        } catch (Exception ex) {
            return InetAddress.getLoopbackAddress().getHostAddress();
        }
    }
}
