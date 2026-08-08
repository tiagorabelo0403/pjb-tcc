package com.tcc.pjb.backend.configs;

import java.util.List;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.tcc.pjb.backend.configs.security.perimeter.SecurityPerimeterProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.tcc.pjb.backend.configs.security.perimeter.PerimeterSecurityFilter;
import com.tcc.pjb.backend.configs.security.perimeter.ForwardedHeaderGuardFilter;
import com.tcc.pjb.backend.configs.security.perimeter.ApiRequestOriginGovernanceFilter;
import com.tcc.pjb.backend.configs.security.hardening.ApiSecurityHardeningFilter;
import com.tcc.pjb.backend.configs.security.hardening.ApiDatabasePressureShieldFilter;
import com.tcc.pjb.backend.configs.security.hardening.ApiDatabasePressureShieldProperties;
import com.tcc.pjb.backend.configs.security.hardening.PjbFunctionalAvailabilityFilter;
import com.tcc.pjb.backend.configs.security.hardening.PjbOperationalAdmissionFilter;
import com.tcc.pjb.backend.configs.security.hardening.PjbOperationalAdmissionProperties;
import com.tcc.pjb.backend.configs.security.hardening.PjbOperationalAdmissionService;
import com.tcc.pjb.backend.configs.security.hardening.PjbFunctionalAvailabilityProperties;
import com.tcc.pjb.backend.configs.security.hardening.ApiLoadSheddingFilter;
import com.tcc.pjb.backend.configs.security.hardening.ApiLoadSheddingProperties;
import com.tcc.pjb.backend.configs.security.hardening.ApiSurfaceProtectionFilter;
import com.tcc.pjb.backend.configs.security.hardening.ApiSurfaceProtectionProperties;
import com.tcc.pjb.backend.configs.security.hardening.SecurityHardeningProperties;
import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceFilter;
import com.tcc.pjb.backend.configs.security.governance.ApiRouteGovernanceProperties;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.systemhealth.PjbOperationalCrisisService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationTokenService;
import com.tcc.pjb.backend.core.security.magistratura.web.DelegationTokenAugmentationFilter;
import com.tcc.pjb.backend.core.security.stepup.web.DecisionStepUpFilter;
import com.tcc.pjb.backend.core.security.stepup.web.DecisionClientBindingFilter;
import com.tcc.pjb.backend.core.security.device.web.DevicePolicyFilter;
import com.tcc.pjb.backend.core.security.device.web.AccountFreezeFilter;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import com.tcc.pjb.backend.core.security.device.SecurityAlertService;
import com.tcc.pjb.backend.core.security.device.TrustedDeviceService;
import com.tcc.pjb.backend.core.security.device.reqhash.RequestHashService;
import com.tcc.pjb.backend.core.security.device.reqhash.RequestBodyHashFilter;
import com.tcc.pjb.backend.core.security.device.reqhash.BodyHashService;
import com.tcc.pjb.backend.platform.security.idempotency.PjbIdempotencyFilter;
import com.tcc.pjb.backend.core.security.webauthn.web.PasskeyAuthenticationFilter;
import com.tcc.pjb.backend.core.security.webauthn.web.MagistraturaIdleLockFilter;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionActivityService;
import com.tcc.pjb.backend.core.security.geofence.web.MagistraturaGeofenceFilter;
import com.tcc.pjb.backend.core.security.geofence.MagistraturaGeofencePolicyService;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.DbUserDetailsService;
import com.tcc.pjb.backend.configs.security.InstitutionalCriticalActionHttpGuardFilter;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimiterStore;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String STRICT_CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'";
    private static final String SWAGGER_UI_CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           UserDetailsService userDetailsService,
                                           ObjectProvider<ForwardedHeaderGuardFilter> forwardedHeaderGuardFilterProvider,
                                           ObjectProvider<PerimeterSecurityFilter> perimeterFilterProvider,
                                           ObjectProvider<ApiSurfaceProtectionFilter> apiSurfaceProtectionFilterProvider,
                                           ObjectProvider<ApiLoadSheddingFilter> apiLoadSheddingFilterProvider,
                                           ObjectProvider<ApiDatabasePressureShieldFilter> apiDatabasePressureShieldFilterProvider,
                                           ObjectProvider<PjbFunctionalAvailabilityFilter> pjbFunctionalAvailabilityFilterProvider,
                                           ObjectProvider<PjbOperationalAdmissionFilter> pjbOperationalAdmissionFilterProvider,
                                           ObjectProvider<ApiSecurityHardeningFilter> apiSecurityHardeningFilterProvider,
                                           ObjectProvider<ApiRouteGovernanceFilter> apiRouteGovernanceFilterProvider,
                                           ObjectProvider<PasskeyAuthenticationFilter> passkeyFilterProvider,
                                           ObjectProvider<MagistraturaIdleLockFilter> magistraturaIdleLockFilterProvider,
                                           ObjectProvider<MagistraturaGeofenceFilter> magistraturaGeofenceFilterProvider,
                                           ObjectProvider<RequestBodyHashFilter> bodyHashFilterProvider,
                                           ObjectProvider<ApiRequestOriginGovernanceFilter> originGovernanceFilterProvider,
                                           ObjectProvider<PjbIdempotencyFilter> pjbIdempotencyFilterProvider,
                                           ObjectProvider<DelegationTokenAugmentationFilter> delegationFilterProvider,
                                           ObjectProvider<DecisionStepUpFilter> decisionStepUpFilterProvider,
                                           ObjectProvider<DecisionClientBindingFilter> decisionClientBindingFilterProvider,
                                           ObjectProvider<AccountFreezeFilter> accountFreezeFilterProvider,
                                           ObjectProvider<DevicePolicyFilter> devicePolicyFilterProvider,
                                           ObjectProvider<InstitutionalCriticalActionHttpGuardFilter> institutionalCriticalActionHttpGuardFilterProvider,
                                           ObjectProvider<JwtDecoder> jwtDecoderProvider,
                                           ApiRouteGovernanceProperties apiRouteGovernanceProperties,
                                           Environment env) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .userDetailsService(userDetailsService);

        ForwardedHeaderGuardFilter forwardedHeaderGuardFilter = forwardedHeaderGuardFilterProvider.getIfAvailable();
        if (forwardedHeaderGuardFilter != null) {
            http.addFilterBefore(forwardedHeaderGuardFilter, BasicAuthenticationFilter.class);
        }

        ApiSurfaceProtectionFilter apiSurfaceProtectionFilter = apiSurfaceProtectionFilterProvider.getIfAvailable();
        if (apiSurfaceProtectionFilter != null) {
            if (forwardedHeaderGuardFilter != null) {
                http.addFilterAfter(apiSurfaceProtectionFilter, ForwardedHeaderGuardFilter.class);
            } else {
                http.addFilterBefore(apiSurfaceProtectionFilter, BasicAuthenticationFilter.class);
            }
        }

        ApiLoadSheddingFilter apiLoadSheddingFilter = apiLoadSheddingFilterProvider.getIfAvailable();
        if (apiLoadSheddingFilter != null) {
            if (apiSurfaceProtectionFilter != null) {
                http.addFilterAfter(apiLoadSheddingFilter, ApiSurfaceProtectionFilter.class);
            } else if (forwardedHeaderGuardFilter != null) {
                http.addFilterAfter(apiLoadSheddingFilter, ForwardedHeaderGuardFilter.class);
            } else {
                http.addFilterBefore(apiLoadSheddingFilter, BasicAuthenticationFilter.class);
            }
        }

        ApiDatabasePressureShieldFilter apiDatabasePressureShieldFilter = apiDatabasePressureShieldFilterProvider.getIfAvailable();
        if (apiDatabasePressureShieldFilter != null) {
            if (apiLoadSheddingFilter != null) {
                http.addFilterAfter(apiDatabasePressureShieldFilter, ApiLoadSheddingFilter.class);
            } else if (apiSurfaceProtectionFilter != null) {
                http.addFilterAfter(apiDatabasePressureShieldFilter, ApiSurfaceProtectionFilter.class);
            } else if (forwardedHeaderGuardFilter != null) {
                http.addFilterAfter(apiDatabasePressureShieldFilter, ForwardedHeaderGuardFilter.class);
            } else {
                http.addFilterBefore(apiDatabasePressureShieldFilter, BasicAuthenticationFilter.class);
            }
        }

        PjbFunctionalAvailabilityFilter pjbFunctionalAvailabilityFilter = pjbFunctionalAvailabilityFilterProvider.getIfAvailable();
        if (pjbFunctionalAvailabilityFilter != null) {
            if (apiDatabasePressureShieldFilter != null) {
                http.addFilterAfter(pjbFunctionalAvailabilityFilter, ApiDatabasePressureShieldFilter.class);
            } else if (apiLoadSheddingFilter != null) {
                http.addFilterAfter(pjbFunctionalAvailabilityFilter, ApiLoadSheddingFilter.class);
            } else if (apiSurfaceProtectionFilter != null) {
                http.addFilterAfter(pjbFunctionalAvailabilityFilter, ApiSurfaceProtectionFilter.class);
            } else if (forwardedHeaderGuardFilter != null) {
                http.addFilterAfter(pjbFunctionalAvailabilityFilter, ForwardedHeaderGuardFilter.class);
            } else {
                http.addFilterBefore(pjbFunctionalAvailabilityFilter, BasicAuthenticationFilter.class);
            }
        }

        PjbOperationalAdmissionFilter pjbOperationalAdmissionFilter = pjbOperationalAdmissionFilterProvider.getIfAvailable();
        if (pjbOperationalAdmissionFilter != null) {
            if (pjbFunctionalAvailabilityFilter != null) {
                http.addFilterAfter(pjbOperationalAdmissionFilter, PjbFunctionalAvailabilityFilter.class);
            } else if (apiDatabasePressureShieldFilter != null) {
                http.addFilterAfter(pjbOperationalAdmissionFilter, ApiDatabasePressureShieldFilter.class);
            } else if (apiLoadSheddingFilter != null) {
                http.addFilterAfter(pjbOperationalAdmissionFilter, ApiLoadSheddingFilter.class);
            } else if (apiSurfaceProtectionFilter != null) {
                http.addFilterAfter(pjbOperationalAdmissionFilter, ApiSurfaceProtectionFilter.class);
            } else if (forwardedHeaderGuardFilter != null) {
                http.addFilterAfter(pjbOperationalAdmissionFilter, ForwardedHeaderGuardFilter.class);
            } else {
                http.addFilterBefore(pjbOperationalAdmissionFilter, BasicAuthenticationFilter.class);
            }
        }

        PerimeterSecurityFilter perimeter = perimeterFilterProvider.getIfAvailable();
        if (perimeter != null) {
            if (pjbOperationalAdmissionFilter != null) {
                http.addFilterAfter(perimeter, PjbOperationalAdmissionFilter.class);
            } else if (pjbFunctionalAvailabilityFilter != null) {
                http.addFilterAfter(perimeter, PjbFunctionalAvailabilityFilter.class);
            } else if (apiDatabasePressureShieldFilter != null) {
                http.addFilterAfter(perimeter, ApiDatabasePressureShieldFilter.class);
            } else if (apiLoadSheddingFilter != null) {
                http.addFilterAfter(perimeter, ApiLoadSheddingFilter.class);
            } else if (apiSurfaceProtectionFilter != null) {
                http.addFilterAfter(perimeter, ApiSurfaceProtectionFilter.class);
            } else if (forwardedHeaderGuardFilter != null) {
                http.addFilterAfter(perimeter, ForwardedHeaderGuardFilter.class);
            } else {
                http.addFilterBefore(perimeter, BasicAuthenticationFilter.class);
            }
        }

        ApiSecurityHardeningFilter apiSecurityHardeningFilter = apiSecurityHardeningFilterProvider.getIfAvailable();
        if (apiSecurityHardeningFilter != null) {
            if (perimeter != null) {
                http.addFilterAfter(apiSecurityHardeningFilter, PerimeterSecurityFilter.class);
            } else if (apiDatabasePressureShieldFilter != null) {
                http.addFilterAfter(apiSecurityHardeningFilter, ApiDatabasePressureShieldFilter.class);
            } else if (apiLoadSheddingFilter != null) {
                http.addFilterAfter(apiSecurityHardeningFilter, ApiLoadSheddingFilter.class);
            } else if (apiSurfaceProtectionFilter != null) {
                http.addFilterAfter(apiSecurityHardeningFilter, ApiSurfaceProtectionFilter.class);
            } else if (forwardedHeaderGuardFilter != null) {
                http.addFilterAfter(apiSecurityHardeningFilter, ForwardedHeaderGuardFilter.class);
            } else {
                http.addFilterBefore(apiSecurityHardeningFilter, BasicAuthenticationFilter.class);
            }
        }

        ApiRouteGovernanceFilter apiRouteGovernanceFilter = apiRouteGovernanceFilterProvider.getIfAvailable();
        if (apiRouteGovernanceFilter != null) {
            if (apiSecurityHardeningFilter != null) {
                http.addFilterAfter(apiRouteGovernanceFilter, ApiSecurityHardeningFilter.class);
            } else if (perimeter != null) {
                http.addFilterAfter(apiRouteGovernanceFilter, PerimeterSecurityFilter.class);
            } else if (apiDatabasePressureShieldFilter != null) {
                http.addFilterAfter(apiRouteGovernanceFilter, ApiDatabasePressureShieldFilter.class);
            } else if (apiLoadSheddingFilter != null) {
                http.addFilterAfter(apiRouteGovernanceFilter, ApiLoadSheddingFilter.class);
            } else if (apiSurfaceProtectionFilter != null) {
                http.addFilterAfter(apiRouteGovernanceFilter, ApiSurfaceProtectionFilter.class);
            } else if (forwardedHeaderGuardFilter != null) {
                http.addFilterAfter(apiRouteGovernanceFilter, ForwardedHeaderGuardFilter.class);
            } else {
                http.addFilterBefore(apiRouteGovernanceFilter, BasicAuthenticationFilter.class);
            }
        }

        PasskeyAuthenticationFilter passkey = passkeyFilterProvider.getIfAvailable();
        if (passkey != null) {
            http.addFilterBefore(passkey, BasicAuthenticationFilter.class);
        }

        MagistraturaIdleLockFilter magistraturaIdleLockFilter = magistraturaIdleLockFilterProvider.getIfAvailable();
        if (magistraturaIdleLockFilter != null && passkey != null) {
            http.addFilterAfter(magistraturaIdleLockFilter, PasskeyAuthenticationFilter.class);
        }

        MagistraturaGeofenceFilter magistraturaGeofenceFilter = magistraturaGeofenceFilterProvider.getIfAvailable();
        if (magistraturaGeofenceFilter != null && passkey != null) {
            if (magistraturaIdleLockFilter != null) {
                http.addFilterAfter(magistraturaGeofenceFilter, MagistraturaIdleLockFilter.class);
            } else {
                http.addFilterAfter(magistraturaGeofenceFilter, PasskeyAuthenticationFilter.class);
            }
        }

        RequestBodyHashFilter bodyHashFilter = bodyHashFilterProvider.getIfAvailable();
        if (bodyHashFilter != null) {
            http.addFilterAfter(bodyHashFilter, BasicAuthenticationFilter.class);
        }

        ApiRequestOriginGovernanceFilter originGovernanceFilter = originGovernanceFilterProvider.getIfAvailable();
        if (originGovernanceFilter != null) {
            if (bodyHashFilter != null) {
                http.addFilterAfter(originGovernanceFilter, RequestBodyHashFilter.class);
            } else {
                http.addFilterAfter(originGovernanceFilter, BasicAuthenticationFilter.class);
            }
        }

        PjbIdempotencyFilter pjbIdempotencyFilter = pjbIdempotencyFilterProvider.getIfAvailable();
        if (pjbIdempotencyFilter != null) {
            if (originGovernanceFilter != null) {
                http.addFilterAfter(pjbIdempotencyFilter, ApiRequestOriginGovernanceFilter.class);
            } else if (bodyHashFilter != null) {
                http.addFilterAfter(pjbIdempotencyFilter, RequestBodyHashFilter.class);
            } else {
                http.addFilterAfter(pjbIdempotencyFilter, BasicAuthenticationFilter.class);
            }
        }

        DelegationTokenAugmentationFilter delegationFilter = delegationFilterProvider.getIfAvailable();
        if (delegationFilter != null) {
            http.addFilterAfter(delegationFilter, BasicAuthenticationFilter.class);
        }
        DecisionStepUpFilter decisionStepUpFilter = decisionStepUpFilterProvider.getIfAvailable();
        if (decisionStepUpFilter != null) {
            if (delegationFilter != null) {
                http.addFilterAfter(decisionStepUpFilter, DelegationTokenAugmentationFilter.class);
            } else {
                http.addFilterAfter(decisionStepUpFilter, BasicAuthenticationFilter.class);
            }
        }
        DecisionClientBindingFilter decisionClientBindingFilter = decisionClientBindingFilterProvider.getIfAvailable();
        if (decisionClientBindingFilter != null) {
            if (decisionStepUpFilter != null) {
                http.addFilterAfter(decisionClientBindingFilter, DecisionStepUpFilter.class);
            } else if (delegationFilter != null) {
                http.addFilterAfter(decisionClientBindingFilter, DelegationTokenAugmentationFilter.class);
            } else {
                http.addFilterAfter(decisionClientBindingFilter, BasicAuthenticationFilter.class);
            }
        }

        AccountFreezeFilter accountFreezeFilter = accountFreezeFilterProvider.getIfAvailable();
        if (accountFreezeFilter != null) {
            http.addFilterAfter(accountFreezeFilter, BasicAuthenticationFilter.class);
        }


        DevicePolicyFilter devicePolicyFilter = devicePolicyFilterProvider.getIfAvailable();
        if (devicePolicyFilter != null) {
            if (bodyHashFilter != null) {
                http.addFilterAfter(devicePolicyFilter, RequestBodyHashFilter.class);
            } else {
                http.addFilterAfter(devicePolicyFilter, BasicAuthenticationFilter.class);
            }
        }

        InstitutionalCriticalActionHttpGuardFilter institutionalCriticalActionHttpGuardFilter = institutionalCriticalActionHttpGuardFilterProvider.getIfAvailable();
        if (institutionalCriticalActionHttpGuardFilter != null) {
            http.addFilterAfter(institutionalCriticalActionHttpGuardFilter, AuthorizationFilter.class);
        }

        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder != null) {
            http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)));
        }

        boolean publicDocs = Boolean.parseBoolean(env.getProperty("pjb.api.docs.public", "false"));

        http.headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .xssProtection(xss -> xss.disable())
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .cacheControl(Customizer.withDefaults())
                .addHeaderWriter((request, response) -> response.setHeader(
                        "Content-Security-Policy",
                        publicDocs && isSwaggerUiPath(request.getServletPath())
                                ? SWAGGER_UI_CONTENT_SECURITY_POLICY
                                : STRICT_CONTENT_SECURITY_POLICY))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()"))
                .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Opener-Policy", "same-origin"))
                .addHeaderWriter(new StaticHeadersWriter("Cross-Origin-Resource-Policy", "same-origin")));

        http.authorizeHttpRequests(authz -> {
                    authz.requestMatchers("/actuator/health", "/actuator/health/**", "/livez", "/readyz", "/startupz").permitAll();
                    authz.requestMatchers("/demo/**")
                            .hasAnyAuthority("ROLE_ADMIN", "ROLE_ADMINISTRADOR");
                    authz.requestMatchers("/api/v1/public/**").permitAll();
                    authz.requestMatchers(
                            "/api/v1/auth/passkey/options",
                            "/api/v1/auth/passkey/finish",
                            "/api/v1/auth/certificado/desafio",
                            "/api/v1/auth/certificado/resposta",
                            "/api/v1/magistratura/ativacao/confirmar").permitAll();
                    authz.requestMatchers("/actuator/info", "/actuator/metrics/**", "/actuator/prometheus")
                            .hasAnyAuthority("ROLE_ADMIN", "ROLE_ADMINISTRADOR");
                    authz.requestMatchers("/api/admin/**", "/api/v1/admin/**")
                            .hasAnyAuthority("ROLE_ADMIN", "ROLE_ADMINISTRADOR");
                    if (publicDocs) {
                        authz.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }
                    if (apiRouteGovernanceProperties != null && apiRouteGovernanceProperties.isEnabled()) {
                        for (ApiRouteGovernanceProperties.Rule rule : apiRouteGovernanceProperties.getRules()) {
                            List<String> paths = normalizeGovernedPaths(rule.getPaths());
                            if (paths.isEmpty() || rule.getAuthorities().isEmpty()) {
                                continue;
                            }
                            authz.requestMatchers(antPathMatchers(paths))
                                    .hasAnyAuthority(rule.getAuthorities().toArray(String[]::new));
                        }
                    }
                    authz.anyRequest().authenticated();
                });

        return http.build();
    }

    @Bean
    public ForwardedHeaderGuardFilter forwardedHeaderGuardFilter(SecurityPerimeterProperties properties,
                                                                 ClientIpResolver clientIpResolver,
                                                                 ObjectMapper objectMapper) {
        return new ForwardedHeaderGuardFilter(properties, clientIpResolver, objectMapper);
    }


    @Bean
    public ApiSurfaceProtectionFilter apiSurfaceProtectionFilter(ApiSurfaceProtectionProperties properties) {
        return new ApiSurfaceProtectionFilter(properties);
    }

    @Bean
    public ApiLoadSheddingFilter apiLoadSheddingFilter(ApiLoadSheddingProperties properties,
                                                        PjbOperationalCrisisService crisisService) {
        return new ApiLoadSheddingFilter(properties, crisisService);
    }

    @Bean
    public ApiDatabasePressureShieldFilter apiDatabasePressureShieldFilter(ApiDatabasePressureShieldProperties properties,
                                                                           @Qualifier("pjbWriteDataSource") ObjectProvider<DataSource> writeDataSourceProvider,
                                                                           @Qualifier("pjbReadDataSource") ObjectProvider<DataSource> readDataSourceProvider,
                                                                           ObjectProvider<DataSource> applicationDataSourceProvider) {
        return new ApiDatabasePressureShieldFilter(properties, writeDataSourceProvider, readDataSourceProvider, applicationDataSourceProvider);
    }

    @Bean
    public PjbFunctionalAvailabilityFilter pjbFunctionalAvailabilityFilter(PjbFunctionalAvailabilityProperties properties,
                                                                           com.tcc.pjb.backend.core.observability.systemhealth.PjbFunctionalAvailabilityService service) {
        return new PjbFunctionalAvailabilityFilter(properties, service);
    }

    @Bean
    public PjbOperationalAdmissionFilter pjbOperationalAdmissionFilter(PjbOperationalAdmissionProperties properties,
                                                                       PjbOperationalAdmissionService service,
                                                                       io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        return new PjbOperationalAdmissionFilter(properties, service, meterRegistry);
    }

    @Bean
    public ApiSecurityHardeningFilter apiSecurityHardeningFilter(SecurityHardeningProperties properties) {
        return new ApiSecurityHardeningFilter(properties);
    }

    @Bean
    public ApiRouteGovernanceFilter apiRouteGovernanceFilter(ApiRouteGovernanceProperties properties,
                                                             ClientIpResolver clientIpResolver,
                                                             RateLimiterStore rateLimiterStore,
                                                             JudicialScaleProfileResolver judicialScaleProfileResolver) {
        return new ApiRouteGovernanceFilter(properties, clientIpResolver, rateLimiterStore, judicialScaleProfileResolver);
    }

    @Bean
    public PasskeyAuthenticationFilter passkeyAuthenticationFilter(PasskeySessionRepository sessionRepo,
                                                                   UserDetailsService userDetailsService) {
        return new PasskeyAuthenticationFilter(sessionRepo, userDetailsService);
    }

    @Bean
    public MagistraturaIdleLockFilter magistraturaIdleLockFilter(PasskeySessionRepository sessionRepo,
                                                                   PasskeySessionActivityService activityService,
                                                                   CurrentUserService currentUserService) {
        return new MagistraturaIdleLockFilter(sessionRepo, activityService, currentUserService);
    }

    @Bean
    public MagistraturaGeofenceFilter magistraturaGeofenceFilter(MagistraturaGeofencePolicyService policyService,
                                                                   CurrentUserService currentUserService,
                                                                   ClientIpResolver clientIpResolver,
                                                                   AuditLedgerService auditLedgerService) {
        return new MagistraturaGeofenceFilter(policyService, currentUserService, clientIpResolver, auditLedgerService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(SecurityPerimeterProperties perimeterProperties, Environment env) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = perimeterProperties.getCorsAllowedOrigins();
        boolean devProfile = env.acceptsProfiles(Profiles.of("local", "dev", "test"));
        List<String> safeOrigins = origins != null && !origins.isEmpty()
                ? origins
                : (devProfile
                    ? List.of("http://localhost:*", "http://127.0.0.1:*", "https://localhost:*", "https://127.0.0.1:*")
                    : List.of());
        config.setAllowedOriginPatterns(safeOrigins);
        config.setAllowedMethods(perimeterProperties.getCorsAllowedMethods());
        config.setAllowedHeaders(perimeterProperties.getCorsAllowedHeaders());
        config.setExposedHeaders(perimeterProperties.getCorsExposedHeaders());
        config.setAllowCredentials(perimeterProperties.isCorsAllowCredentials() && origins != null && !origins.contains("*"));
        config.setMaxAge(perimeterProperties.getCorsMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private static List<String> normalizeGovernedPaths(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static RequestMatcher[] antPathMatchers(List<String> paths) {
        return paths.stream()
                .map(AntPathRequestMatcher::new)
                .toArray(RequestMatcher[]::new);
    }

    private static boolean isSwaggerUiPath(String path) {
        return "/swagger-ui.html".equals(path) || path.startsWith("/swagger-ui/");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return new DbUserDetailsService(usuarioRepository);
    }
}
