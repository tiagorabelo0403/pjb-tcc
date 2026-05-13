package com.tcc.pjb.backend.core.json;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
public class StrictJacksonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer strictJacksonCustomizer() {
    return builder -> builder.postConfigurer(this::configure);
  }

  private void configure(ObjectMapper mapper) {
    mapper.setConfig(mapper.getDeserializationConfig()
        .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .without(MapperFeature.DEFAULT_VIEW_INCLUSION));
    mapper.setConfig(mapper.getSerializationConfig()
        .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .without(MapperFeature.DEFAULT_VIEW_INCLUSION));
    applyConstraints(mapper.getFactory());
  }

  private void applyConstraints(JsonFactory factory) {
    factory.setStreamReadConstraints(StreamReadConstraints.builder()
        .maxNestingDepth(200)
        .maxStringLength(200_000)
        .maxNumberLength(1000)
        .build());
  }
}
