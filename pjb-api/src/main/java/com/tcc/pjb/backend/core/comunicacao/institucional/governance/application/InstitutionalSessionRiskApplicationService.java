package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalSessionRiskAssessmentStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class InstitutionalSessionRiskApplicationService {

    private static final ZoneId BRAZIL_DEFAULT_ZONE = ZoneId.of("America/Fortaleza");

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalSessionRiskAssessmentStateRepository repository;
    private final InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService;

    public InstitutionalSessionRiskApplicationService(CurrentUserService currentUserService,
                                                      InstitutionalAffiliationStateRepository affiliationRepository,
                                                      InstitutionalNominationStateRepository nominationRepository,
                                                      InstitutionalSessionRiskAssessmentStateRepository repository,
                                                      InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.repository = Objects.requireNonNull(repository);
        this.remoteCertificateAuthorizationApplicationService = Objects.requireNonNull(remoteCertificateAuthorizationApplicationService);
    }

    public InstitutionalSessionRiskAssessment avaliarAtual(String affiliationId,
                                                           String nominationId,
                                                           String unidadeCodigo,
                                                           String caixaCodigo) {
        Usuario user = currentUserService.getRequired();
        InstitutionalAffiliation affiliation = affiliationId == null || affiliationId.isBlank() ? null : affiliationRepository.findByAffiliationId(affiliationId).orElse(null);
        InstitutionalNomination nomination = nominationId == null || nominationId.isBlank() ? null : nominationRepository.findByNominationId(nominationId).orElse(null);
        if (nomination == null && user.getId() != null) {
            nomination = nominationRepository.findByNominatedUserId(user.getId()).stream()
                    .filter(item -> affiliation == null || item.affiliationId().equals(affiliation.affiliationId()))
                    .sorted(Comparator.comparing(InstitutionalNomination::updatedAt).reversed())
                    .findFirst()
                    .orElse(null);
        }
        return avaliarAtual(user, affiliation, nomination, unidadeCodigo, caixaCodigo);
    }

    public InstitutionalSessionRiskAssessment avaliarAtual(Usuario user,
                                                           InstitutionalAffiliation affiliation,
                                                           InstitutionalNomination nomination,
                                                           String unidadeCodigo,
                                                           String caixaCodigo) {
        HttpServletRequest request = currentRequest();
        String requestedUnit = firstNonBlank(unidadeCodigo, header(request, "X-PJB-REQUEST-UNIT"), nomination == null ? null : nomination.unidadeCodigo());
        String requestedBox = firstNonBlank(caixaCodigo, header(request, "X-PJB-REQUEST-BOX"), nomination == null ? null : nomination.caixaCodigo());
        String deviceId = firstNonBlank(header(request, "X-PJB-DEVICE-ID"), header(request, "X-DEVICE-ID"));
        String ip = request == null ? null : request.getRemoteAddr();
        String geoUf = firstNonBlank(header(request, "X-PJB-GEO-UF"), header(request, "X-GEO-UF"));
        int mfaFailures = parseInt(header(request, "X-PJB-MFA-FAILURES-LAST-24H"));
        int burst = parseInt(header(request, "X-PJB-OPS-BURST-MINUTE"));
        List<InstitutionalSessionRiskAssessment> history = user == null || user.getId() == null ? List.of() : repository.findByUserId(user.getId());
        ArrayList<InstitutionalSessionRiskFinding> findings = new ArrayList<>();

        if (deviceId == null || deviceId.isBlank()) {
            findings.add(finding("DEVICE_ID_AUSENTE", InstitutionalRiskSeverity.MEDIA, false,
                    "Sessão sem identificação explícita de dispositivo homologado.", List.of()));
        } else if (history.stream().noneMatch(item -> deviceId.equals(item.deviceId()))) {
            findings.add(finding("NOVO_DISPOSITIVO", InstitutionalRiskSeverity.MEDIA, false,
                    "Sessão em dispositivo ainda não observado no histórico institucional recente.", List.of("deviceId=" + deviceId)));
        }

        if (LocalTime.now(BRAZIL_DEFAULT_ZONE).isBefore(LocalTime.of(6, 0)) || LocalTime.now(BRAZIL_DEFAULT_ZONE).isAfter(LocalTime.of(22, 0))) {
            findings.add(finding("HORARIO_ANOMALO", InstitutionalRiskSeverity.MEDIA, false,
                    "Sessão institucional em horário fora da janela operacional padrão.", List.of("hora=" + LocalTime.now(BRAZIL_DEFAULT_ZONE))));
        }

        if (mfaFailures >= 3) {
            findings.add(finding("MULTIPLAS_FALHAS_MFA", InstitutionalRiskSeverity.ALTA, true,
                    "Múltiplas falhas de MFA recentes exigem contenção e revalidação manual.", List.of("falhas24h=" + mfaFailures)));
        }

        if (burst >= 40) {
            findings.add(finding("EXPLOSAO_OPERACOES", InstitutionalRiskSeverity.ALTA, true,
                    "Volume anômalo de operações por minuto indica necessidade de aprovação manual ou bloqueio temporário.", List.of("opsPerMinute=" + burst)));
        } else if (burst >= 20) {
            findings.add(finding("PICO_OPERACIONAL", InstitutionalRiskSeverity.MEDIA, false,
                    "Pico operacional acima do padrão histórico da sessão atual.", List.of("opsPerMinute=" + burst)));
        }

        if (nomination != null && requestedUnit != null && !requestedUnit.equalsIgnoreCase(nomination.unidadeCodigo())) {
            findings.add(finding("TROCA_SUSPEITA_UNIDADE", InstitutionalRiskSeverity.ALTA, true,
                    "A unidade requerida na sessão não coincide com a unidade da nomeação ativa.", List.of("requestedUnit=" + requestedUnit, "nominationUnit=" + nomination.unidadeCodigo())));
        }
        if (nomination != null && requestedBox != null && !requestedBox.equalsIgnoreCase(nomination.caixaCodigo())) {
            findings.add(finding("TROCA_SUSPEITA_CAIXA", InstitutionalRiskSeverity.ALTA, true,
                    "A caixa requerida na sessão não coincide com a caixa da nomeação ativa.", List.of("requestedBox=" + requestedBox, "nominationBox=" + nomination.caixaCodigo())));
        }

        if (affiliation != null && geoUf != null && affiliation.uf() != null && !geoUf.equalsIgnoreCase(affiliation.uf())) {
            findings.add(finding("UF_ANOMALA", InstitutionalRiskSeverity.MEDIA, false,
                    "A sessão parte de UF distinta da abrangência principal da afiliação institucional.", List.of("geoUf=" + geoUf, "affiliationUf=" + affiliation.uf())));
        }

        boolean remoteAuthActive = user != null && affiliation != null
                && remoteCertificateAuthorizationApplicationService.possuiAutorizacaoAtiva(user.getId(), affiliation.affiliationId());
        boolean networkTrusted = headerEquals(request, "X-PJB-INSTITUTIONAL-NETWORK", "trusted") || isPrivateNetwork(ip);
        if (!networkTrusted && affiliation != null && affiliation.restringeCertificadoRedeInstitucional() && !remoteAuthActive) {
            findings.add(finding("CERTIFICADO_FORA_DA_REDE_SEM_AUTORIZACAO", InstitutionalRiskSeverity.CRITICA, true,
                    "Uso de certificado fora da rede institucional sem autorização remota ativa e auditável.", List.of("affiliationId=" + affiliation.affiliationId())));
        }

        int score = findings.stream().mapToInt(item -> item.severity().weight()).sum();
        boolean blocked = findings.stream().anyMatch(InstitutionalSessionRiskFinding::blocking)
                || findings.stream().anyMatch(item -> item.severity().isBlocking());
        boolean requiresManualApproval = !blocked && score >= 50;
        boolean requiresStepUp = !blocked && score >= 25;
        String level = blocked ? "CRITICO" : score >= 75 ? "ALTO" : score >= 25 ? "MODERADO" : "BAIXO";
        InstitutionalSessionRiskAssessment assessment = new InstitutionalSessionRiskAssessment(
                UUID.randomUUID().toString(),
                user == null ? null : user.getId(),
                user == null ? null : user.getNome(),
                affiliation == null ? null : affiliation.affiliationId(),
                nomination == null ? null : nomination.nominationId(),
                requestedUnit,
                requestedBox,
                deviceId,
                ip,
                geoUf,
                score,
                level,
                requiresStepUp,
                requiresManualApproval,
                blocked,
                List.copyOf(findings),
                List.of(
                        "motor_de_risco_de_sessao_institucional",
                        "historico_considerado=" + history.size(),
                        "networkTrusted=" + networkTrusted,
                        "remoteAuthActive=" + remoteAuthActive),
                Instant.now(),
                null
        );
        return repository.save(assessment);
    }

    private InstitutionalSessionRiskFinding finding(String code,
                                                    InstitutionalRiskSeverity severity,
                                                    boolean blocking,
                                                    String message,
                                                    List<String> evidences) {
        return new InstitutionalSessionRiskFinding(code, severity, blocking, message, evidences);
    }

    private boolean isPrivateNetwork(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.2")
                || ip.startsWith("127.")
                || ip.equals("::1")
                || ip.equals("0:0:0:0:0:0:0:1");
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }

    private String header(HttpServletRequest request, String name) {
        return request == null ? null : request.getHeader(name);
    }

    private boolean headerEquals(HttpServletRequest request, String name, String expected) {
        String value = header(request, name);
        return value != null && value.equalsIgnoreCase(expected);
    }

    private int parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String firstNonBlank(String... values) {
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
}
