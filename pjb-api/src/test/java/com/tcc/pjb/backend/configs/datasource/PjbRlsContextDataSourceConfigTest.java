package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.EquipeFiltroContextoQuery;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * Prova que, quando pjb.datasource.routing.enabled=false (o padrão herdado por todo ambiente que
 * não usa a topologia HA/read-replica), o DataSource de fallback continua respeitando
 * spring.datasource.hikari.* (maximum-pool-size etc.) — achado da revisão profunda: a primeira
 * versão do fallback perdia esse tuning silenciosamente, caindo nos defaults genéricos do Hikari.
 */
class PjbRlsContextDataSourceConfigTest {

    @Test
    void semRoutingAtivo_fallbackRespeitaTuningDoHikari() {
        new ApplicationContextRunner()
                .withUserConfiguration(GuardSupportConfig.class, PjbRlsContextDataSourceConfig.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/nao-usado",
                        "spring.datasource.username=pjb",
                        "spring.datasource.password=pjb",
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.datasource.hikari.maximum-pool-size=7",
                        "spring.datasource.hikari.minimum-idle=2",
                        "spring.datasource.hikari.pool-name=pjb-fallback-test"
                )
                .run(context -> {
                    assertThat(context).hasBean("pjbFallbackDataSource");
                    HikariDataSource fallback = context.getBean("pjbFallbackDataSource", HikariDataSource.class);
                    assertThat(fallback.getMaximumPoolSize()).isEqualTo(7);
                    assertThat(fallback.getMinimumIdle()).isEqualTo(2);
                    assertThat(fallback.getPoolName()).isEqualTo("pjb-fallback-test");

                    DataSource primary = context.getBean("dataSource", DataSource.class);
                    assertThat(primary).isInstanceOf(LazyConnectionDataSourceProxy.class);
                });
    }

    @Configuration
    static class GuardSupportConfig {

        @Bean
        DataSourceProperties dataSourceProperties() {
            return new DataSourceProperties();
        }

        @Bean
        PjbProcessoSigiloRlsContext pjbProcessoSigiloRlsContext() {
            return new PjbProcessoSigiloRlsContext();
        }

        @Bean
        EquipeFiltroContextoQuery equipeFiltroContextoQuery() {
            return new EquipeFiltroContextoQuery();
        }

        @Bean
        PjbRlsEquipeResolver pjbRlsEquipeResolver(EquipeFiltroContextoQuery query) {
            return new PjbRlsEquipeResolver(query);
        }

        @Bean
        PjbRlsActorResolver pjbRlsActorResolver() {
            return new PjbRlsActorResolver();
        }
    }
}
