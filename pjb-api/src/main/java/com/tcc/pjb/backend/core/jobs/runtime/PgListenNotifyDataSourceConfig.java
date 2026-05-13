package com.tcc.pjb.backend.core.jobs.runtime;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.jobs.pg-listen", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("'${spring.datasource.url:}'.startsWith('jdbc:postgresql:')")
public class PgListenNotifyDataSourceConfig {

    @Bean
    @Qualifier("pgListenDataSource")
    public DataSource pgListenDataSource(DataSourceProperties props) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(props.determineUrl());
        ds.setUser(props.determineUsername());
        ds.setPassword(props.determinePassword());
        return ds;
    }
}
