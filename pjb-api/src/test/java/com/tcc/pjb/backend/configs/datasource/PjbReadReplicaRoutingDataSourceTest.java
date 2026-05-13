package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class PjbReadReplicaRoutingDataSourceTest {

    @Test
    void shouldPreferWriteWhenAdaptivePlaneForcesPrimary() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbAdaptiveDataPlaneContext adaptiveContext = new PjbAdaptiveDataPlaneContext();
        PjbReadReplicaRoutingDataSource dataSource = new PjbReadReplicaRoutingDataSource(context, Set.of("READ_NORDESTE"), new PjbDataSourceRoutingProperties(), adaptiveContext);
        adaptiveContext.bind(new PjbAdaptiveDataPlaneService.AdaptiveDecision(
                PjbAdaptiveDataPlaneService.AdaptiveMode.PRIMARY_STRICT,
                "critical-read-route-with-replica-lag",
                true,
                false,
                false,
                false,
                4.2d,
                0.10d,
                0.20d,
                0,
                0,
                null,
                null,
                false,
                "VARA_1G",
                "PRIMEIRA_INSTANCIA",
                "ESTADUAL"
        ));
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            assertThat(dataSource.determineCurrentLookupKey()).isEqualTo(PjbDataSourceRole.WRITE);
        } finally {
            adaptiveContext.clear();
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        }
    }


    @Test
    void shouldUseAdaptivePreferredReplicaWhenPresent() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbAdaptiveDataPlaneContext adaptiveContext = new PjbAdaptiveDataPlaneContext();
        PjbReadReplicaRoutingDataSource dataSource = new PjbReadReplicaRoutingDataSource(context, Set.of("READ_SUDESTE", "READ_NORDESTE"), new PjbDataSourceRoutingProperties(), adaptiveContext);
        adaptiveContext.bind(new PjbAdaptiveDataPlaneService.AdaptiveDecision(
                PjbAdaptiveDataPlaneService.AdaptiveMode.REPLICA_REGIONAL,
                "fallback-replica-healthy",
                false,
                false,
                false,
                false,
                0.4d,
                0.10d,
                0.20d,
                0,
                0,
                "READ_SUDESTE",
                "NORDESTE",
                true,
                "SECRETARIA_TRIBUNAL",
                "SEGUNDA_INSTANCIA",
                "FEDERAL"
        ));
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            assertThat(dataSource.determineCurrentLookupKey()).isEqualTo("READ_SUDESTE");
        } finally {
            adaptiveContext.clear();
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        }
    }


    @Test
    void shouldPreferWriteWhenPrimaryPreferenceIsActive() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbReadReplicaRoutingDataSource dataSource = new PjbReadReplicaRoutingDataSource(context);
        context.preferPrimaryFor(java.time.Duration.ofSeconds(1));
        assertThat(dataSource.determineCurrentLookupKey()).isEqualTo(PjbDataSourceRole.WRITE);
    }

    @Test
    void shouldUseReadForReadOnlyTransactionWithoutPrimaryPreference() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbReadReplicaRoutingDataSource dataSource = new PjbReadReplicaRoutingDataSource(context);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            assertThat(dataSource.determineCurrentLookupKey()).isEqualTo(PjbDataSourceRole.READ);
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        }
    }

    @Test
    void shouldUseRegionalReplicaFromRequestHeaders() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        PjbReadReplicaRoutingDataSource dataSource = new PjbReadReplicaRoutingDataSource(context, Set.of("READ_NORDESTE", "READ_SUPERIOR"), properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getRegionalSelection().getRequestHeaderUf(), "CE");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            assertThat(dataSource.determineCurrentLookupKey()).isEqualTo("READ_NORDESTE");
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            RequestContextHolder.resetRequestAttributes();
            SecurityContextHolder.clearContext();
        }
    }


    @Test
    void shouldUseRegionalReplicaMappedByInstitutionalOrgaoHeader() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        properties.getRegionalSelection().getOrgaoToReplica().put("MPCE", "NORDESTE");
        PjbReadReplicaRoutingDataSource dataSource = new PjbReadReplicaRoutingDataSource(context, Set.of("READ_NORDESTE", "READ_SUPERIOR"), properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getRegionalSelection().getRequestHeaderOrgao(), "MPCE");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            assertThat(dataSource.determineCurrentLookupKey()).isEqualTo("READ_NORDESTE");
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            RequestContextHolder.resetRequestAttributes();
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldUseRegionalReplicaMappedByInstitutionalUnitAndBoxBeforeTribunal() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        properties.getRegionalSelection().getUnidadeToReplica().put("UNID-DF-1", "CENTRO_OESTE");
        properties.getRegionalSelection().getCaixaToReplica().put("CX-DF-9", "CENTRO_OESTE");
        properties.getRegionalSelection().getTribunalToReplica().put("TJCE", "NORDESTE");
        PjbReadReplicaRoutingDataSource dataSource = new PjbReadReplicaRoutingDataSource(context, Set.of("READ_NORDESTE", "READ_CENTRO_OESTE"), properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getRegionalSelection().getRequestHeaderTribunal(), "TJCE");
        request.addHeader(properties.getRegionalSelection().getRequestHeaderUnidade(), "UNID-DF-1");
        request.addHeader(properties.getRegionalSelection().getRequestHeaderCaixa(), "CX-DF-9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            assertThat(dataSource.determineCurrentLookupKey()).isEqualTo("READ_CENTRO_OESTE");
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            RequestContextHolder.resetRequestAttributes();
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldInferRegionalReplicaFromInstitutionalCodeSuffixWhenMapIsAbsent() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        PjbDataSourceRoutingProperties properties = new PjbDataSourceRoutingProperties();
        PjbReadReplicaRoutingDataSource dataSource = new PjbReadReplicaRoutingDataSource(context, Set.of("READ_NORDESTE", "READ_SUPERIOR"), properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(properties.getRegionalSelection().getRequestHeaderOrgao(), "DPECE");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        try {
            assertThat(dataSource.determineCurrentLookupKey()).isEqualTo("READ_NORDESTE");
        } finally {
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
            RequestContextHolder.resetRequestAttributes();
            SecurityContextHolder.clearContext();
        }
    }
}
