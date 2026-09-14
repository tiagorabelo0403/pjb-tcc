package com.tcc.pjb.backend.integration.govbr.demoidp;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Servidor de autorização OIDC real (Spring Authorization Server) que faz o papel do
 * gov.br Login Único somente em ambiente de demonstração, enquanto a credencial oficial
 * (client_id/secret) não chega do processo de credenciamento em acesso.gov.br.
 * {@link com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcClient} continua sendo o
 * mesmo cliente usado contra o gov.br real — nenhuma linha dele muda para este modo existir.
 */
// mock-enabled=true sozinho é o default de application-test.yml para toda a suíte (cobre
// GovBrMockSignatureService, sem SecurityFilterChain próprio). Exigir enabled=true também evita
// que este SecurityFilterChain adicional se registre em slices de teste que não o esperam —
// só application-demo.yml liga os dois juntos.
@Configuration
@Profile({"dev", "test", "demo"})
@ConditionalOnProperty(prefix = "pjb.integrations.govbr", name = {"enabled", "mock-enabled"}, havingValue = "true", matchIfMissing = false)
public class GovBrDemoIdentityProviderConfig {

  @Bean
  @Order(1)
  public SecurityFilterChain govBrDemoAuthorizationServerSecurityFilterChain(
      HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        OAuth2AuthorizationServerConfigurer.authorizationServer();

    http
        .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        .with(authorizationServerConfigurer, authorizationServer ->
            authorizationServer.oidc(Customizer.withDefaults()))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .csrf(csrf -> csrf.disable())
        .userDetailsService(userDetailsService)
        .formLogin(Customizer.withDefaults())
        .exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();
  }

  @Bean
  public RegisteredClientRepository govBrDemoRegisteredClientRepository(GovBrOidcProperties props,
      PasswordEncoder passwordEncoder) {
    List<String> redirectUris = new ArrayList<>();
    addIfPresent(redirectUris, props.redirectUri());
    addIfPresent(redirectUris, props.effectiveLoginRedirectUri());
    addIfPresent(redirectUris, props.effectiveStepUpRedirectUri());

    RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId(props.clientId())
        .clientSecret(passwordEncoder.encode(props.clientSecret()))
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .scope("openid")
        .scope("email")
        .scope("profile")
        .scope("govbr_confiabilidades")
        .clientSettings(ClientSettings.builder()
            .requireProofKey(true)
            .requireAuthorizationConsent(false)
            .build());
    redirectUris.forEach(builder::redirectUri);

    return new InMemoryRegisteredClientRepository(builder.build());
  }

  @Bean
  public JWKSource<SecurityContext> govBrDemoJwkSource() {
    KeyPair keyPair = generateRsaKey();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAKey rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
    return new ImmutableJWKSet<>(new JWKSet(rsaKey));
  }

  @Bean
  public AuthorizationServerSettings govBrDemoAuthorizationServerSettings(GovBrOidcProperties props) {
    return AuthorizationServerSettings.builder()
        .issuer(props.issuer())
        .build();
  }

  private static void addIfPresent(List<String> target, String value) {
    if (value != null && !value.isBlank() && !target.contains(value.trim())) {
      target.add(value.trim());
    }
  }

  private static KeyPair generateRsaKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      return keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException("govbr_demo_rsa_key_generation_failed", ex);
    }
  }
}
