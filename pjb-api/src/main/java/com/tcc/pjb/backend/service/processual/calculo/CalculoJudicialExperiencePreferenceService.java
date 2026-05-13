package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperiencePreferenceRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperiencePreferenceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.entity.ui.UsuarioCalculoExperiencePreference;
import com.tcc.pjb.backend.repository.ui.UsuarioCalculoExperiencePreferenceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CalculoJudicialExperiencePreferenceService {

  private static final String TEAM_POLICY_PRINCIPAL = "__TEAM_POLICY__";

  private final UsuarioCalculoExperiencePreferenceRepository repository;
  private final CalculoJudicialFrontendContractService frontendContractService;

  public CalculoJudicialExperiencePreferenceService(UsuarioCalculoExperiencePreferenceRepository repository,
                                                    CalculoJudicialFrontendContractService frontendContractService) {
    this.repository = Objects.requireNonNull(repository);
    this.frontendContractService = Objects.requireNonNull(frontendContractService);
  }

  @Transactional(readOnly = true)
  public CalculoJudicialExperiencePreferenceResponse resolve(Authentication authentication, CalculoJudicialSolicitantePerfil perfil) {
    return resolve(authentication, perfil, null, null);
  }

  @Transactional(readOnly = true)
  public CalculoJudicialExperiencePreferenceResponse resolve(Authentication authentication, CalculoJudicialSolicitantePerfil perfil, String domainCode) {
    return resolve(authentication, perfil, domainCode, null);
  }

  @Transactional(readOnly = true)
  public CalculoJudicialExperiencePreferenceResponse resolve(Authentication authentication,
                                                             CalculoJudicialSolicitantePerfil perfil,
                                                             String domainCode,
                                                             CalculoJudicialExperienceContext context) {
    CalculoJudicialSolicitantePerfil effectiveProfile = Objects.requireNonNullElse(perfil, CalculoJudicialSolicitantePerfil.CIDADAO);
    String canonicalDomain = canonicalOrNull(domainCode);
    ExperienceContext normalizedContext = normalizeContext(context);
    String defaultMode = contextualDefaultMode(effectiveProfile, normalizedContext);
    String principalKey = principalKey(authentication);
    Long equipeAtivaId = equipeAtivaId();

    Resolution resolution = resolvePreference(principalKey, equipeAtivaId, canonicalDomain, normalizedContext);
    String mode = resolution.entity() == null ? defaultMode : sanitizeMode(resolution.entity().getExperienceMode(), defaultMode);
    String source = resolution.entity() == null ? "PROFILE_DEFAULT" : resolution.source();
    UsuarioCalculoExperiencePreference entity = resolution.entity();

    return new CalculoJudicialExperiencePreferenceResponse(
            mode,
            source,
            entity != null && entity.getEquipeAtivaId() != null,
            canonicalDomain != null,
            resolution.institutionalPolicyApplied(),
            entity == null ? equipeAtivaId : entity.getEquipeAtivaId(),
            canonicalDomain,
            principalKey == null ? "anon" : principalKey,
            selector(mode, effectiveProfile, principalKey, equipeAtivaId, canonicalDomain, source, resolution.institutionalPolicyApplied(), normalizedContext),
            normalizedContext.toMap(),
            entity == null ? Instant.now() : entity.getUpdatedAt()
    );
  }

  @Transactional
  public CalculoJudicialExperiencePreferenceResponse save(Authentication authentication,
                                                          CalculoJudicialSolicitantePerfil perfil,
                                                          CalculoJudicialExperiencePreferenceRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("preferencia_experience_request_obrigatoria");
    }
    if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
      throw new IllegalStateException("preferencia_experience_requires_authenticated_user");
    }
    CalculoJudicialSolicitantePerfil effectiveProfile = Objects.requireNonNullElse(perfil, CalculoJudicialSolicitantePerfil.CIDADAO);
    ExperienceContext context = normalizeContext(new CalculoJudicialExperienceContext(request.ramoDireito(), request.classeProcessual(), request.tipoCausa(), request.perfilEquipe(), request.tribunal(), request.sistemaOrigem()));
    String defaultMode = contextualDefaultMode(effectiveProfile, context);
    String principalKey = principalKey(authentication);
    Long equipeAtivaId = request.persistForTeam() ? equipeAtivaId() : null;
    String canonicalDomain = canonicalOrNull(request.domainCode());
    boolean institutionalPolicy = request.persistForTeam() && request.institutionalPolicy() && equipeAtivaId != null;
    String scope = resolveScopeType(equipeAtivaId, institutionalPolicy, context);
    String mode = sanitizeMode(request.experienceMode(), defaultMode);
    String persistencePrincipal = institutionalPolicy ? TEAM_POLICY_PRINCIPAL : principalKey;

    UsuarioCalculoExperiencePreference entity = findForSave(persistencePrincipal, equipeAtivaId, canonicalDomain, context, institutionalPolicy)
            .orElseGet(() -> new UsuarioCalculoExperiencePreference(persistencePrincipal, equipeAtivaId, canonicalDomain, mode, scope, context.ramoDireito(), context.classeProcessual(), context.tipoCausa(), context.perfilEquipe(), context.tribunal(), context.sistemaOrigem(), institutionalPolicy));
    entity.setPrincipalKey(persistencePrincipal);
    entity.setEquipeAtivaId(equipeAtivaId);
    entity.setDomainCode(canonicalDomain);
    entity.setExperienceMode(mode);
    entity.setScopeType(scope);
    entity.setRamoDireito(context.ramoDireito());
    entity.setClasseProcessual(context.classeProcessual());
    entity.setTipoCausa(context.tipoCausa());
    entity.setPerfilEquipe(context.perfilEquipe());
    entity.setTribunal(context.tribunal());
    entity.setSistemaOrigem(context.sistemaOrigem());
    entity.setInstitutionalPolicy(institutionalPolicy);
    entity.setUpdatedAt(Instant.now());
    UsuarioCalculoExperiencePreference saved = repository.save(entity);

    String source = institutionalPolicy ? "TEAM_POLICY" : equipeAtivaId == null ? "USER_PREFERENCE" : "TEAM_PREFERENCE";
    return new CalculoJudicialExperiencePreferenceResponse(
            saved.getExperienceMode(),
            source,
            equipeAtivaId != null,
            canonicalDomain != null,
            institutionalPolicy,
            equipeAtivaId,
            canonicalDomain,
            principalKey,
            selector(saved.getExperienceMode(), effectiveProfile, principalKey, equipeAtivaId, canonicalDomain, source, institutionalPolicy, context),
            context.toMap(),
            saved.getUpdatedAt()
    );
  }

  public String sanitizeMode(String mode, String fallback) {
    if (mode == null || mode.isBlank()) {
      return fallback;
    }
    String normalized = mode.trim().toLowerCase(Locale.ROOT);
    if ("manual_tradicional".equals(normalized) || "assistido_com_ia".equals(normalized)) {
      return normalized;
    }
    return fallback;
  }

  public Map<String, Object> resolvedModesByDomain(Authentication authentication,
                                                   CalculoJudicialSolicitantePerfil perfil) {
    return resolvedModesByDomain(authentication, perfil, null);
  }

  public Map<String, Object> resolvedModesByDomain(Authentication authentication,
                                                   CalculoJudicialSolicitantePerfil perfil,
                                                   CalculoJudicialExperienceContext context) {
    Map<String, Object> resolved = new LinkedHashMap<>();
    resolved.put("GLOBAL", resolve(authentication, perfil, null, context));
    for (String domain : CalculoJudicialDomainSupport.supportedDomains()) {
      resolved.put(domain, resolve(authentication, perfil, domain, context));
    }
    return Map.copyOf(resolved);
  }

  private Resolution resolvePreference(String principalKey, Long equipeAtivaId, String canonicalDomain, ExperienceContext context) {
    if (principalKey == null) {
      return new Resolution(null, "PROFILE_DEFAULT", false);
    }
    List<UsuarioCalculoExperiencePreference> teamUser = equipeAtivaId == null ? List.of() : nullSafeList(repository.findAllByPrincipalKeyAndEquipeAtivaIdOrderByUpdatedAtDescIdDesc(principalKey, equipeAtivaId));
    List<UsuarioCalculoExperiencePreference> globalUser = nullSafeList(repository.findAllByPrincipalKeyAndEquipeAtivaIdIsNullOrderByUpdatedAtDescIdDesc(principalKey));
    List<UsuarioCalculoExperiencePreference> teamPolicy = equipeAtivaId == null ? List.of() : nullSafeList(repository.findAllByPrincipalKeyAndEquipeAtivaIdOrderByUpdatedAtDescIdDesc(TEAM_POLICY_PRINCIPAL, equipeAtivaId));

    List<Resolution> candidates = List.of(
            resolution(bestMatch(teamUser, canonicalDomain, context, false, true), "TEAM_DOMAIN_USER_PREFERENCE", false),
            resolution(bestMatch(teamPolicy, canonicalDomain, context, true, true), "TEAM_DOMAIN_POLICY", true),
            resolution(bestMatch(globalUser, canonicalDomain, context, false, true), "USER_DOMAIN_PREFERENCE", false),
            resolution(bestMatch(teamUser, null, context, false, false), "TEAM_GLOBAL_USER_PREFERENCE", false),
            resolution(bestMatch(teamPolicy, null, context, true, false), "TEAM_GLOBAL_POLICY", true),
            resolution(bestMatch(globalUser, null, context, false, false), "USER_GLOBAL_PREFERENCE", false)
    );
    return candidates.stream().filter(candidate -> candidate.entity() != null).findFirst().orElse(new Resolution(null, "PROFILE_DEFAULT", false));
  }

  private Optional<UsuarioCalculoExperiencePreference> findForSave(String principalKey,
                                                                   Long equipeAtivaId,
                                                                   String canonicalDomain,
                                                                   ExperienceContext context,
                                                                   boolean institutionalPolicy) {
    List<UsuarioCalculoExperiencePreference> pool = equipeAtivaId == null
            ? nullSafeList(repository.findAllByPrincipalKeyAndEquipeAtivaIdIsNullOrderByUpdatedAtDescIdDesc(principalKey))
            : nullSafeList(repository.findAllByPrincipalKeyAndEquipeAtivaIdOrderByUpdatedAtDescIdDesc(principalKey, equipeAtivaId));
    return pool.stream()
            .filter(entity -> institutionalPolicy == entity.isInstitutionalPolicy())
            .filter(entity -> Objects.equals(normalizeNullable(entity.getDomainCode()), canonicalDomain))
            .filter(entity -> Objects.equals(normalizeNullable(entity.getRamoDireito()), context.ramoDireito()))
            .filter(entity -> Objects.equals(normalizeNullable(entity.getClasseProcessual()), context.classeProcessual()))
            .filter(entity -> Objects.equals(normalizeNullable(entity.getTipoCausa()), context.tipoCausa()))
            .filter(entity -> Objects.equals(normalizeNullable(entity.getPerfilEquipe()), context.perfilEquipe()))
            .filter(entity -> Objects.equals(normalizeNullable(entity.getTribunal()), context.tribunal()))
            .filter(entity -> Objects.equals(normalizeNullable(entity.getSistemaOrigem()), context.sistemaOrigem()))
            .findFirst();
  }

  private UsuarioCalculoExperiencePreference bestMatch(List<UsuarioCalculoExperiencePreference> candidates,
                                                       String canonicalDomain,
                                                       ExperienceContext context,
                                                       boolean institutionalPolicy,
                                                       boolean requireDomain) {
    return candidates.stream()
            .filter(entity -> entity.isInstitutionalPolicy() == institutionalPolicy)
            .filter(entity -> requireDomain ? Objects.equals(normalizeNullable(entity.getDomainCode()), canonicalDomain) : entity.getDomainCode() == null)
            .filter(entity -> contextMatches(entity, context))
            .max(Comparator.comparingInt((UsuarioCalculoExperiencePreference entity) -> contextSpecificity(entity)).thenComparing(UsuarioCalculoExperiencePreference::getUpdatedAt))
            .orElse(null);
  }

  private boolean contextMatches(UsuarioCalculoExperiencePreference entity, ExperienceContext context) {
    return matchesNullable(entity.getRamoDireito(), context.ramoDireito())
            && matchesNullable(entity.getClasseProcessual(), context.classeProcessual())
            && matchesNullable(entity.getTipoCausa(), context.tipoCausa())
            && matchesNullable(entity.getPerfilEquipe(), context.perfilEquipe())
            && matchesNullable(entity.getTribunal(), context.tribunal())
            && matchesNullable(entity.getSistemaOrigem(), context.sistemaOrigem());
  }

  private int contextSpecificity(UsuarioCalculoExperiencePreference entity) {
    int score = 0;
    if (entity.getRamoDireito() != null) {
      score++;
    }
    if (entity.getClasseProcessual() != null) {
      score++;
    }
    if (entity.getTipoCausa() != null) {
      score++;
    }
    if (entity.getPerfilEquipe() != null) {
      score++;
    }
    if (entity.getTribunal() != null) {
      score++;
    }
    if (entity.getSistemaOrigem() != null) {
      score++;
    }
    return score;
  }

  private boolean matchesNullable(String persisted, String requested) {
    String left = normalizeNullable(persisted);
    if (left == null) {
      return true;
    }
    return Objects.equals(left, requested);
  }

  private String resolveScopeType(Long equipeAtivaId, boolean institutionalPolicy, ExperienceContext context) {
    boolean contextual = context.ramoDireito() != null || context.classeProcessual() != null || context.tipoCausa() != null || context.perfilEquipe() != null || context.tribunal() != null || context.sistemaOrigem() != null;
    if (institutionalPolicy) {
      return contextual ? "TEAM_POLICY_CONTEXT" : "TEAM_POLICY";
    }
    if (equipeAtivaId != null) {
      return contextual ? "TEAM_CONTEXT" : "TEAM";
    }
    return contextual ? "USER_CONTEXT" : "USER";
  }

  private ExperienceContext normalizeContext(CalculoJudicialExperienceContext context) {
    if (context == null) {
      return new ExperienceContext(null, null, null, null, null, null);
    }
    return new ExperienceContext(
            normalizeNullable(context.ramoDireito()),
            normalizeNullable(context.classeProcessual()),
            normalizeNullable(context.tipoCausa()),
            normalizeNullable(context.perfilEquipe()),
            normalizeNullable(context.tribunal()),
            normalizeNullable(context.sistemaOrigem())
    );
  }


  private String contextualDefaultMode(CalculoJudicialSolicitantePerfil perfil, ExperienceContext context) {
    String fallback = frontendContractService.defaultExperienceMode(perfil);
    if (context == null) {
      return fallback;
    }
    if (containsManualFirst(context.perfilEquipe()) || containsManualFirst(context.tribunal()) || containsManualFirst(context.sistemaOrigem())) {
      return "manual_tradicional";
    }
    return fallback;
  }

  private boolean containsManualFirst(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String normalized = value.toUpperCase(Locale.ROOT);
    return normalized.contains("CONTENCIOSO_MASSIFICADO")
            || normalized.contains("CONTADORIA")
            || normalized.contains("CALCULOS")
            || normalized.contains("GABINETE")
            || normalized.contains("TRIAGEM_TECNICA")
            || normalized.contains("MIGRACAO_LEGADO")
            || normalized.contains("SAJ")
            || normalized.contains("EPROC")
            || normalized.contains("PROJUDI")
            || normalized.contains("CRETA");
  }
  private Map<String, Object> selector(String resolvedMode,
                                       CalculoJudicialSolicitantePerfil perfil,
                                       String principalKey,
                                       Long equipeAtivaId,
                                       String canonicalDomain,
                                       String source,
                                       boolean institutionalPolicyApplied,
                                       ExperienceContext context) {
    Map<String, Object> selector = new LinkedHashMap<>();
    selector.put("resolvedMode", resolvedMode);
    selector.put("defaultMode", contextualDefaultMode(perfil, context));
    selector.put("modes", frontendContractService.experienceModes(canonicalDomain, perfil));
    selector.put("selectorVisible", Boolean.TRUE);
    selector.put("selectorMessage", frontendContractService.modeSelectorMessage(perfil));
    selector.put("savePreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute());
    selector.put("loadPreferenceRoute", CalculoJudicialDomainSupport.experiencePreferenceRoute(canonicalDomain));
    selector.put("supportsTeamScope", equipeAtivaId != null);
    selector.put("supportsInstitutionalTeamPolicy", equipeAtivaId != null);
    selector.put("supportsPolicyContext", Boolean.TRUE);
    selector.put("policyContextFields", frontendContractService.experiencePreferenceContextFields());
    selector.put("principalKey", principalKey == null ? "anon" : principalKey);
    selector.put("equipeAtivaId", equipeAtivaId);
    selector.put("domainCode", canonicalDomain);
    selector.put("source", source);
    selector.put("institutionalPolicyApplied", institutionalPolicyApplied);
    selector.put("policyContext", context.toMap());
    selector.entrySet().removeIf(entry -> entry.getValue() == null);
    return Map.copyOf(selector);
  }

  private String principalKey(Authentication authentication) {
    if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
      return null;
    }
    return authentication.getName().trim();
  }

  private Long equipeAtivaId() {
    HttpServletRequest request = currentRequest();
    if (request == null) {
      return null;
    }
    String raw = request.getHeader(EquipeSwitchInterceptor.HEADER_EQUIPE_ID);
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(raw.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private HttpServletRequest currentRequest() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes instanceof ServletRequestAttributes servlet) {
      return servlet.getRequest();
    }
    return null;
  }

  private String canonicalOrNull(String domainCode) {
    if (domainCode == null || domainCode.isBlank()) {
      return null;
    }
    return CalculoJudicialDomainSupport.requireSupported(domainCode);
  }

  private <T> List<T> nullSafeList(List<T> value) {
    return value == null ? List.of() : List.copyOf(value);
  }

  private String normalizeNullable(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .replace('-', '_')
            .replace('/', '_')
            .replace(' ', '_')
            .toUpperCase(Locale.ROOT)
            .replaceAll("_+", "_");
  }

  private Resolution resolution(UsuarioCalculoExperiencePreference entity, String source, boolean institutionalPolicyApplied) {
    return new Resolution(entity, source, institutionalPolicyApplied);
  }

  private record ExperienceContext(String ramoDireito, String classeProcessual, String tipoCausa, String perfilEquipe, String tribunal, String sistemaOrigem) {
    private Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("ramoDireito", ramoDireito);
      map.put("classeProcessual", classeProcessual);
      map.put("tipoCausa", tipoCausa);
      map.put("perfilEquipe", perfilEquipe);
      map.put("tribunal", tribunal);
      map.put("sistemaOrigem", sistemaOrigem);
      map.entrySet().removeIf(entry -> entry.getValue() == null);
      return Map.copyOf(map);
    }
  }

  private record Resolution(UsuarioCalculoExperiencePreference entity, String source, boolean institutionalPolicyApplied) {
  }
}
