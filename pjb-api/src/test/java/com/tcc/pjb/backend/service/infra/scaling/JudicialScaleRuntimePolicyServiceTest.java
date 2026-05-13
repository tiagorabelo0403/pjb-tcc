package com.tcc.pjb.backend.service.infra.scaling;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.configs.kafka.PjbKafkaScaleProperties;
import java.time.Year;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class JudicialScaleRuntimePolicyServiceTest {

    @Test
    void shouldExposeCollegiateAndSeasonalMetadataForElectoralSecondInstance() {
        JudicialScaleProfileResolver resolver = new JudicialScaleProfileResolver();
        PjbDataSourceRoutingProperties routingProperties = new PjbDataSourceRoutingProperties();
        PjbKafkaScaleProperties kafkaProperties = new PjbKafkaScaleProperties();
        kafkaProperties.setListenerConcurrency(4);
        StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("kafkaScaleProperties", kafkaProperties);
        Environment environment = new MockEnvironment().withProperty("pjb.outbox.poller.batch", "50");
        JudicialScaleRuntimePolicyService service = new JudicialScaleRuntimePolicyService(
                resolver,
                routingProperties,
                factory.getBeanProvider(PjbKafkaScaleProperties.class),
                environment
        );

        JudicialScaleRuntimePolicyService.JudicialRuntimePolicyView view = service.preview("SEGUNDA_INSTANCIA", "ELEITORAL");

        assertThat(view.instanceClass()).isEqualTo("SEGUNDA_INSTANCIA");
        assertThat(view.branchClass()).isEqualTo("ELEITORAL");
        assertThat(view.metadata()).containsEntry("collegiateMode", true);
        assertThat(view.metadata()).containsEntry(
                "seasonalMode",
                Year.now().getValue() % 2 == 0 ? "ELEICAO" : "PADRAO"
        );
        assertThat(view.schedulerPollMs()).isPositive();
        assertThat(view.kafkaListenerConcurrency()).isPositive();
    }
}
