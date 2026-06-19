package com.tcc.pjb.backend.configs.kafka;

import com.tcc.pjb.backend.query.consumer.ProcessoMaterializadoConsumer;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.calendar.CalendarNotificationEventPublisher;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaNotificationEventPublisher;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara todos os tópicos Kafka do PJB com particionamento explícito.
 *
 * O número de partições é derivado de {@code pjb.kafka.scale.listener-concurrency}
 * para garantir que toda thread de consumer tenha uma partição disponível.
 * KafkaAdmin cria os tópicos no startup se não existirem; nunca reduz partições
 * em tópicos existentes.
 */
@Configuration
@ConditionalOnProperty(name = "pjb.kafka.enabled", havingValue = "true")
public class PjbKafkaTopicConfig {

    private final int partitions;
    private final String readModelsTopic;

    public PjbKafkaTopicConfig(
            PjbKafkaScaleProperties scaleProperties,
            @Value("${pjb.datasource.routing.processual-read-models.kafka-topic:pjb.processual.read-models.v1}")
            String readModelsTopic) {
        this.partitions = Math.max(1, scaleProperties.getListenerConcurrency());
        this.readModelsTopic = readModelsTopic;
    }

    @Bean
    public NewTopic topicCalendarioNotificacao() {
        return TopicBuilder.name(CalendarNotificationEventPublisher.TOPIC)
                .partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic topicOficialJusticaNotificacao() {
        return TopicBuilder.name(OficialJusticaNotificationEventPublisher.TOPIC)
                .partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic topicProcessoMaterializado() {
        return TopicBuilder.name(ProcessoMaterializadoConsumer.TOPIC)
                .partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic topicFederacaoJudicial() {
        return TopicBuilder.name(FederalismoJudicialEngine.TOPIC_FEDERACAO_EVENTOS)
                .partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic topicPainelNacional() {
        return TopicBuilder.name("pjb.nacional.painel.eventos.v1")
                .partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic topicProntuarioNacional() {
        return TopicBuilder.name(ProntuarioNacionalService.TOPIC_PRONTUARIO_NACIONAL)
                .partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic topicProcessualReadModels() {
        return TopicBuilder.name(readModelsTopic)
                .partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic topicUsuarioAtividade() {
        return TopicBuilder.name("evento.usuario.atividade.v1")
                .partitions(partitions).replicas(1).build();
    }
}
