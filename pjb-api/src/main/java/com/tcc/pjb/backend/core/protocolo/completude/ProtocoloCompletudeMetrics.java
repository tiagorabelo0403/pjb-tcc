package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.OrigemValidacao;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoDocumentoProcessual;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProtocoloCompletudeMetrics {

    private final MeterRegistry registry;

    public ProtocoloCompletudeMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
    }

    public void registrarBloqueado(RitoProcessual rito) {
        Counter.builder("pjb.protocolo.completude.bloqueados")
                .description("Protocolos bloqueados por completude documental insuficiente")
                .tag("rito", rito != null ? rito.name() : "DESCONHECIDO")
                .register(registry)
                .increment();
    }

    public void registrarLiberado(RitoProcessual rito) {
        Counter.builder("pjb.protocolo.completude.liberados")
                .description("Protocolos liberados pelo gate de completude")
                .tag("rito", rito != null ? rito.name() : "DESCONHECIDO")
                .register(registry)
                .increment();
    }

    public void registrarViolacaoTipoDoc(TipoDocumentoProcessual tipo) {
        Counter.builder("pjb.protocolo.completude.pendencia_tipo_doc")
                .description("Violações por tipo de documento faltante")
                .tag("tipo_documento", tipo != null ? tipo.name() : "DESCONHECIDO")
                .register(registry)
                .increment();
    }

    public void registrarOverride(RitoProcessual rito) {
        Counter.builder("pjb.protocolo.completude.override")
                .description("Overrides de completude concedidos pela secretaria")
                .tag("rito", rito != null ? rito.name() : "DESCONHECIDO")
                .register(registry)
                .increment();
    }

    public void registrarValidacao(OrigemValidacao origem) {
        Counter.builder("pjb.protocolo.completude.validacoes")
                .description("Total de validações de completude executadas")
                .tag("origem", origem != null ? origem.name() : "DESCONHECIDO")
                .register(registry)
                .increment();
    }

    public void registrarFalhaOcr() {
        Counter.builder("pjb.protocolo.completude.ocr_falhas")
                .description("Falhas de OCR/análise inteligente de documento")
                .register(registry)
                .increment();
    }

    public void registrarTempoRegularizacao(Duration tempo) {
        Timer.builder("pjb.protocolo.completude.regularizacao_tempo")
                .description("Tempo entre criação da pendência e regularização")
                .register(registry)
                .record(tempo);
    }
}
