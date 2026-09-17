package com.tcc.pjb.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import com.tcc.pjb.backend.configs.live.LiveClusterStateStoreConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

// JacksonAutoConfiguration (tools.jackson / Jackson 3) excluida: o projeto inteiro usa
// com.fasterxml.jackson (Jackson 2, ver StrictJacksonConfig). Com as duas linhas do Jackson no
// classpath, ela tenta ligar sozinha e falha o boot ao vincular "spring.jackson.serialization.
// write-dates-as-timestamps" (propriedade da linha 2) contra o tipo SerializationFeature (um
// enum) da linha 3, que nem tem essa constante. Excluir devolve o conversor HTTP JSON para o
// Jackson2HttpMessageConvertersConfiguration (spring-boot-jackson2), que e o que o projeto usa.
//
// @EnableCaching mora em PjbCacheConfig, nao aqui: classes anotadas na config principal (esta)
// sao aplicadas mesmo dentro de fatias @WebMvcTest (que so filtram config espalhada pelo
// component scan comum). Ate o Boot 4.0.x, @WebMvcTest vinha com @AutoConfigureCache embutido e
// cobria isso sozinho; o Boot 4.1.1 tirou essa composicao, entao @EnableCaching aqui passou a
// exigir CacheManager em toda fatia @WebMvcTest do projeto (nenhuma delas usa cache de verdade).
@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = JacksonAutoConfiguration.class)
@Import(LiveClusterStateStoreConfiguration.class)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}