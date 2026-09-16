package com.tcc.pjb.backend.modules.support;

import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

// Registrada em META-INF/spring/org.springframework.boot.webmvc.test.autoconfigure
// .AutoConfigureMockMvc.imports, entao vale para todo teste com @AutoConfigureMockMvc (direto ou
// via @WebMvcTest, que e composto por ela). Boot 4.1.1 parou de aplicar springSecurity() sozinho
// no MockMvc (existia em MockMvcSecurityConfiguration ate o Boot 3.5.x, removida sem
// substituta automatica). Sem isso, @WithMockUser fica sem efeito: SecurityContextHolderFilter
// le o contexto de um RequestAttributeSecurityContextRepository vazio, cai pra
// AnonymousAuthenticationToken e authorizeHttpRequests/@PreAuthorize barram tudo com 401/403.
@Configuration(proxyBeanMethods = false)
public class SecurityMockMvcAutoConfiguration {

    @Bean
    public MockMvcBuilderCustomizer securityMockMvcBuilderCustomizer() {
        return builder -> builder.apply(SecurityMockMvcConfigurers.springSecurity());
    }
}
