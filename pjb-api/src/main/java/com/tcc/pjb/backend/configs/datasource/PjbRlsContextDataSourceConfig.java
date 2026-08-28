package com.tcc.pjb.backend.configs.datasource;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * Publica o {@code @Primary DataSource} da aplicação, sempre embrulhado pelo backstop de RLS
 * ({@link PjbProcessoSigiloRlsDataSource}) — independente de {@code pjb.datasource.routing.enabled}
 * estar ligado.
 *
 * <p>Antes desta classe, o wrapper de RLS só existia dentro do {@code @Bean} de
 * {@code PjbReadWriteDataSourceConfig.dataSource(...)}, e aquela classe inteira é
 * {@code @ConditionalOnProperty(pjb.datasource.routing.enabled=true)} — flag que é {@code false}
 * por padrão e não é ligada no {@code docker-compose.yml} base nem no overlay k8s {@code prod}
 * "liso" (só em topologias HA/read-replica). Na prática, isso deixava as GUCs
 * {@code app.pjb_actor_id}/{@code app.pjb_actor_roles} — e portanto toda policy de RLS que depende
 * delas (V343 e as que vieram depois) — sem nenhuma conexão real as populando no deploy padrão.</p>
 *
 * <p>Esta classe compõe com o datasource de routing quando ele existe ({@code pjbRoutingComposedDataSource},
 * via {@link ObjectProvider}), ou com um datasource simples construído de
 * {@code spring.datasource.*} quando não existe.</p>
 */
@Configuration
public class PjbRlsContextDataSourceConfig {

    /**
     * Datasource simples (spring.datasource.*), só criado quando o routing (que já publica seu
     * próprio pjbWriteDataSource) não está ativo. O bind manual de {@code spring.datasource.hikari}
     * via {@link Binder} (em vez de {@code @ConfigurationProperties} na assinatura do método) é
     * necessário porque {@code PjbReadWriteDataSourceConfig.pjbWriteDataSource} já reivindica esse
     * mesmo prefixo — duas anotações {@code @ConfigurationProperties} com o mesmo prefixo no
     * classpath é erro de COMPILAÇÃO, mesmo os dois beans sendo mutuamente exclusivos em runtime
     * via {@code @ConditionalOnMissingBean}. Sem esse bind (achado da revisão profunda: a primeira
     * versão do fallback não fazia bind nenhum), o datasource cairia nos defaults genéricos do
     * Hikari, silenciosamente, no deploy padrão (routing desligado).
     */
    @Bean(name = "pjbFallbackDataSource")
    @ConditionalOnMissingBean(name = "pjbRoutingComposedDataSource")
    public HikariDataSource pjbFallbackDataSource(DataSourceProperties properties, Environment environment) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        Binder.get(environment).bind("spring.datasource.hikari", Bindable.ofInstance(dataSource));
        return dataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("pjbRoutingComposedDataSource") ObjectProvider<DataSource> routingComposedDataSource,
                                 @Qualifier("pjbFallbackDataSource") ObjectProvider<DataSource> fallbackDataSource,
                                 PjbProcessoSigiloRlsContext processoSigiloRlsContext,
                                 PjbRlsActorResolver rlsActorResolver,
                                 PjbRlsEquipeResolver rlsEquipeResolver) {
        DataSource base = routingComposedDataSource.getIfAvailable(fallbackDataSource::getObject);
        DataSource sigiloAwareRouting = new PjbProcessoSigiloRlsDataSource(base, processoSigiloRlsContext, rlsActorResolver, rlsEquipeResolver);
        return new LazyConnectionDataSourceProxy(sigiloAwareRouting);
    }
}
