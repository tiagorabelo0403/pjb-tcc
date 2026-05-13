package com.tcc.pjb.backend.service.juiz.guardrails;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;

@Service
public class JuizProcessoGuardRailService {

    private final CurrentUserService currentUserService;
    private final UserPersonaService personaService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final SecretariatOperationalRoutingResolver routingResolver;

    public JuizProcessoGuardRailService(CurrentUserService currentUserService,
                                        UserPersonaService personaService,
                                        ProcessoRepository processoRepository,
                                        WorkItemRepository workItemRepository,
                                        SecretariatOperationalRoutingResolver routingResolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.personaService = Objects.requireNonNull(personaService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.routingResolver = Objects.requireNonNull(routingResolver);
    }

    @Transactional(readOnly = true)
    public GuardRailSnapshot avaliar(Long processoId, String action) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        return avaliar(processo, currentUserService.getRequired(), personaService.getRequiredPersona(), action);
    }

    @Transactional(readOnly = true)
    public GuardRailSnapshot avaliar(Processo processo, Usuario usuario, UserPersona persona, String action) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(persona, "persona");
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        List<GuardSignal> signals = new ArrayList<>();
        signals.add(signal("ROLE_MAGISTRATURA", usuario.isMagistrado(), "CRITICA",
                "Ator precisa pertencer à magistratura para atuar no processo."));
        signals.add(signal("ESFERA_COMPATIVEL", isJusticeCompatible(usuario.getTipoUsuario(), routing), "CRITICA",
                "A esfera/justiça do processo deve ser compatível com o perfil do magistrado."));
        signals.add(signal("GRAU_COMPATIVEL", isGradeCompatible(persona, routing), "CRITICA",
                "O grau de atuação do magistrado deve coincidir com a instância do processo."));
        signals.add(signal("REGIME_COMPATIVEL", isRegimeCompatible(usuario.getTipoUsuario(), routing), "CRITICA",
                "O regime do processo não pode escapar da trilha permitida ao perfil do magistrado."));
        signals.add(signal("TERRITORIO_COMPATIVEL", isTerritoryCompatible(usuario, processo, routing, persona), "ALTA",
                "UF, comarca ou sede competente do processo devem estar dentro do território operacional do magistrado."));
        signals.add(signal("RAMO_COMPATIVEL", isRamoCompatible(usuario, processo, routing), "ALTA",
                "A matéria processual deve respeitar as especialidades e a lane operacional do magistrado."));
        signals.add(signal("SIGILO_GERENCIAVEL", isSecrecyManageable(usuario, processo), "ALTA",
                "Processo sigiloso exige contexto judicial compatível e não deve cair em atuação insegura."));
        signals.add(signal("CONCORRENCIA_CONTROLADA", isConcurrencyControlled(processo, usuario), "MEDIA",
                "O processo não deve estar sob trilha decisória ativa de outro magistrado incompatível."));
        signals.add(signal("SECRETARIA_DESTINO_COERENTE", hasCoherentSecretariatDestination(routing), "MEDIA",
                "Despachos, sentenças e pautas precisam desaguar na secretaria competente já roteada pelo PJB."));
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("O guard rail judicial cruza tipo de justiça, instância, território, regime e lane processual antes de permitir atuação do magistrado.");
        fundamentos.add("Topologia processual resolvida: " + routing.organizationalPath() + ".");
        fundamentos.add("Secretaria de destino associada ao processo: " + routing.secretariatCode() + ".");
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            fundamentos.add("O processo possui sigilo e exige coerência reforçada entre magistrado, lane e secretaria receptora.");
        }
        if (routing.regimeAxis() != null && routing.regimeAxis().contains("JUIZADO")) {
            fundamentos.add("O regime de juizado permanece isolado do procedimento comum e não pode ser capturado por gabinete errado.");
        }
        if (routing.instanciaAxis() != null && routing.instanciaAxis().contains("SEGUNDO")) {
            fundamentos.add("Fluxo colegiado precisa permanecer separado do gabinete singular de primeiro grau.");
        }
        boolean allowed = signals.stream().filter(signal -> signal.blocking()).allMatch(GuardSignal::satisfied);
        String verdict = allowed
                ? signals.stream().anyMatch(signal -> !signal.satisfied()) ? "VIGILANCIA" : "LIBERADO"
                : "BLOQUEADO";
        LinkedHashSet<String> violations = new LinkedHashSet<>();
        for (GuardSignal signal : signals) {
            if (!signal.satisfied()) {
                violations.add(signal.code());
            }
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("processoId", processo.getId());
        metrics.put("numeroProcesso", firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()));
        metrics.put("routingPath", routing.organizationalPath());
        metrics.put("routeKey", routing.routeKey());
        metrics.put("tipoJustica", routing.tipoJustica());
        metrics.put("instanciaAxis", routing.instanciaAxis());
        metrics.put("regimeAxis", routing.regimeAxis());
        metrics.put("ramoAxis", routing.ramoAxis());
        metrics.put("secretariatCode", routing.secretariatCode());
        metrics.put("usuarioId", usuario.getId());
        metrics.put("usuarioTipo", usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name());
        metrics.put("personaKey", persona.personaKey().name());
        metrics.put("personaGrau", persona.grau() == null ? null : persona.grau().name());
        metrics.put("personaEsfera", persona.esfera() == null ? null : persona.esfera().name());
        metrics.put("violations", List.copyOf(violations));
        metrics.put("allowed", allowed);
        return new GuardRailSnapshot(
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                firstNonBlank(action, "ATUACAO_JUDICIAL"),
                allowed,
                verdict,
                routing,
                List.copyOf(signals),
                List.copyOf(fundamentos),
                Map.copyOf(metrics)
        );
    }

    public GuardRailSnapshot requireAtuacaoPermitida(Processo processo, Usuario usuario, String action) {
        GuardRailSnapshot snapshot = avaliar(processo, usuario, personaService.getRequiredPersona(), action);
        if (!snapshot.allowed()) {
            String motivo = snapshot.signals().stream()
                    .filter(signal -> signal.blocking() && !signal.satisfied())
                    .map(signal -> signal.code() + ": " + signal.message())
                    .findFirst()
                    .orElse("Processo fora do contexto operacional do magistrado.");
            throw new AccessDeniedPjbException(motivo);
        }
        return snapshot;
    }

    private boolean isJusticeCompatible(TipoUsuario tipo, SecretariatOperationalRoutingProfile routing) {
        if (tipo == null) {
            return false;
        }
        TipoJustica justica = TipoJustica.fromString(routing.tipoJustica());
        if (tipo == TipoUsuario.MINISTRO || tipo == TipoUsuario.DESEMBARGADOR || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            return true;
        }
        return switch (tipo) {
            case JUIZ_FEDERAL -> justica == TipoJustica.FEDERAL;
            case JUIZ_ELEITORAL -> justica == TipoJustica.ELEITORAL;
            case JUIZ_TRABALHISTA -> justica == TipoJustica.TRABALHO;
            case JUIZ_MILITAR -> justica == TipoJustica.MILITAR_ESTADUAL || justica == TipoJustica.MILITAR_FEDERAL;
            case JUIZ_ESTADUAL, JUIZ, JUIZ_ESPECIAL, MAGISTRADO -> justica == null || justica == TipoJustica.ESTADUAL || justica == TipoJustica.SUPERIOR && tipo == TipoUsuario.MAGISTRADO;
            default -> false;
        };
    }

    private boolean isGradeCompatible(UserPersona persona, SecretariatOperationalRoutingProfile routing) {
        if (persona.grau() == null || routing.instanciaAxis() == null) {
            return true;
        }
        return switch (routing.instanciaAxis()) {
            case "PRIMEIRO_GRAU" -> persona.grau() == GrauJurisdicao.PRIMEIRO_GRAU;
            case "SEGUNDO_GRAU" -> persona.grau() == GrauJurisdicao.SEGUNDO_GRAU || persona.grau() == GrauJurisdicao.SUPERIOR;
            case "TRIBUNAL_SUPERIOR" -> persona.grau() == GrauJurisdicao.SUPERIOR;
            default -> true;
        };
    }

    private boolean isRegimeCompatible(TipoUsuario tipo, SecretariatOperationalRoutingProfile routing) {
        if (tipo == null) {
            return false;
        }
        String regime = normalize(routing.regimeAxis());
        if (tipo == TipoUsuario.JUIZ_ESPECIAL) {
            return regime.contains("JUIZADO_ESPECIAL");
        }
        if (tipo == TipoUsuario.JUIZ_FEDERAL) {
            return regime.contains("JUSTICA_FEDERAL") || regime.contains("JUIZADO_ESPECIAL_FEDERAL");
        }
        if (tipo == TipoUsuario.JUIZ_ELEITORAL) {
            return regime.contains("JUSTICA_ELEITORAL");
        }
        if (tipo == TipoUsuario.JUIZ_TRABALHISTA) {
            return regime.contains("JUSTICA_TRABALHO");
        }
        if (tipo == TipoUsuario.JUIZ_MILITAR) {
            return regime.contains("JUSTICA_MILITAR");
        }
        return !regime.contains("TRIBUNAL_SUPERIOR") || tipo == TipoUsuario.MINISTRO;
    }

    private boolean isTerritoryCompatible(Usuario usuario,
                                          Processo processo,
                                          SecretariatOperationalRoutingProfile routing,
                                          UserPersona persona) {
        if (persona.grau() != GrauJurisdicao.PRIMEIRO_GRAU) {
            return true;
        }
        String userUf = normalize(usuario.getUf());
        String processUf = normalize(firstNonBlank(processo.getUf(), stringFromMap(routing.metadata(), "uf")));
        if (!userUf.isBlank() && !processUf.isBlank() && !Objects.equals(userUf, processUf)) {
            return false;
        }
        if (usuario.getComarca() == null || usuario.getComarca().isBlank()) {
            return true;
        }
        String userComarca = normalize(usuario.getComarca());
        String processComarca = normalize(firstNonBlank(
                processo.getComarca(),
                stringFromNestedMap(routing.metadata(), "topology", "coverage", "seatMunicipality"),
                stringFromNestedMap(routing.metadata(), "topology", "coverage", "territorialScope"),
                stringFromNestedMap(routing.metadata(), "topology", "unitDescriptor")
        ));
        if (processComarca.isBlank()) {
            return true;
        }
        return processComarca.equals(userComarca) || processComarca.contains(userComarca) || userComarca.contains(processComarca);
    }

    private boolean isRamoCompatible(Usuario usuario, Processo processo, SecretariatOperationalRoutingProfile routing) {
        String lane = normalize(routing.ramoAxis());
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == TipoUsuario.JUIZ_ELEITORAL) {
            return lane.contains("ELEITORAL");
        }
        if (tipo == TipoUsuario.JUIZ_TRABALHISTA) {
            return lane.contains("TRABALHISTA");
        }
        if (tipo == TipoUsuario.JUIZ_MILITAR) {
            return lane.contains("MILITAR") || lane.contains("PENAL");
        }
        if (tipo == TipoUsuario.JUIZ_FEDERAL && processo.getTipoJustica() == TipoJustica.FEDERAL) {
            return true;
        }
        if (!usuario.getEspecialidades().isEmpty()) {
            return usuario.possuiEspecialidade(routing.ramoAxis()) || usuario.possuiEspecialidade(processo.getClasseProcessual()) || usuario.possuiEspecialidade(processo.getAssunto());
        }
        return true;
    }

    private boolean isSecrecyManageable(Usuario usuario, Processo processo) {
        return processo.getNivelSigilo() == null
                || processo.getNivelSigilo() == NivelSigilo.PUBLICO
                || usuario.isMagistrado();
    }

    private boolean isConcurrencyControlled(Processo processo, Usuario usuario) {
        List<WorkItem> items = workItemRepository.findAllByProcesso(processo.getId());
        Optional<WorkItem> foreign = items.stream()
                .filter(item -> item.getAssignedUser() != null)
                .filter(item -> item.getAssignedUser().isMagistrado())
                .filter(item -> !Objects.equals(item.getAssignedUser().getId(), usuario.getId()))
                .filter(item -> item.getStatus() != null && item.getStatus() != com.tcc.pjb.backend.model.entity.enums.WorkItemStatus.CANCELADO)
                .findFirst();
        return foreign.isEmpty();
    }

    private boolean hasCoherentSecretariatDestination(SecretariatOperationalRoutingProfile routing) {
        return notBlank(routing.secretariatCode())
                && notBlank(routing.executionInboxKey())
                && notBlank(routing.executionQueueCode())
                && notBlank(routing.audienceInboxKey())
                && notBlank(routing.audienceQueueCode());
    }

    private GuardSignal signal(String code, boolean satisfied, String level, String message) {
        boolean blocking = "CRITICA".equals(level) || "ALTA".equals(level);
        return new GuardSignal(code, level, blocking, satisfied, message);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
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

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    private String stringFromMap(Map<String, Object> metadata, String key) {
        if (metadata == null || key == null) {
            return null;
        }
        Object value = metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private String stringFromNestedMap(Map<String, Object> metadata, String first, String key) {
        if (metadata == null || first == null || key == null) {
            return null;
        }
        Object firstValue = metadata.get(first);
        if (!(firstValue instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object value = firstMap.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String stringFromNestedMap(Map<String, Object> metadata, String first, String second, String key) {
        if (metadata == null || first == null) {
            return null;
        }
        Object firstValue = metadata.get(first);
        if (!(firstValue instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object secondValue = firstMap.get(second);
        if (!(secondValue instanceof Map<?, ?> secondMap)) {
            return null;
        }
        Object value = secondMap.get(key);
        return value == null ? null : String.valueOf(value);
    }

    public record GuardRailSnapshot(
            Long processoId,
            String numeroProcesso,
            String action,
            boolean allowed,
            String verdictBand,
            SecretariatOperationalRoutingProfile routing,
            List<GuardSignal> signals,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record GuardSignal(
            String code,
            String level,
            boolean blocking,
            boolean satisfied,
            String message
    ) {
    }
}
