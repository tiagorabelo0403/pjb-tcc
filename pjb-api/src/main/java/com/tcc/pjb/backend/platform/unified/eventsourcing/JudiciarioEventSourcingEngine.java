package com.tcc.pjb.backend.platform.unified.eventsourcing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventStore;
import com.tcc.pjb.backend.core.kernel.process.ProcessEventType;
import com.tcc.pjb.backend.model.entity.kernel.ProcessEventEnvelope;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;

@Service
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class JudiciarioEventSourcingEngine {

    private static final Duration DISPATCH_TIMEOUT = Duration.ofSeconds(8);

    public sealed interface Comando permits AutuarProcesso, JuntarDocumentoMassivo, ProferirDecisao {
        long processoId();
    }

    public record AutuarProcesso(long processoId, String cpfCnpjAutor, String jurisdicao, String rito) implements Comando {
    }

    public record JuntarDocumentoMassivo(long processoId, List<DocumentoMetadata> documentos) implements Comando {
    }

    public record ProferirDecisao(long processoId, String teor, UUID magistradoId) implements Comando {
    }

    public record DocumentoMetadata(UUID docId, String storageUri, String hashSha384, long tamanhoBytes) {
    }

    public sealed interface EventoJudicial permits ProcessoAutuado, DocumentosJuntados, DecisaoProferida {
        UUID eventoId();

        long processoId();

        Instant timestamp();
    }

    public record ProcessoAutuado(UUID eventoId, long processoId, String cpfCnpjAutor, String jurisdicao, String rito, Instant timestamp) implements EventoJudicial {
    }

    public record DocumentosJuntados(UUID eventoId, long processoId, List<DocumentoMetadata> documentos, Instant timestamp) implements EventoJudicial {
    }

    public record DecisaoProferida(UUID eventoId, long processoId, byte[] hashDecisao, UUID magistradoId, Instant timestamp) implements EventoJudicial {
    }

    public static final class ProcessoAggregate {
        private final long id;
        private long versao = 0;
        private boolean autuado = false;
        private final List<DocumentoMetadata> documentosAtivos = new ArrayList<>();
        private final List<EventoJudicial> mudancasNaoSalvas = new ArrayList<>();

        public ProcessoAggregate(long id) {
            this.id = id;
        }

        public void carregarHistorico(List<EventoJudicial> historico) {
            for (EventoJudicial evento : historico) {
                aplicarMutacao(evento);
                versao++;
            }
        }

        public void processar(Comando comando) {
            switch (comando) {
                case AutuarProcesso c -> {
                    if (autuado) throw new IllegalStateException("Processo já autuado");
                    aplicarNovoEvento(new ProcessoAutuado(UUID.randomUUID(), id, c.cpfCnpjAutor(), c.jurisdicao(), c.rito(), Instant.now()));
                }
                case JuntarDocumentoMassivo c -> {
                    if (!autuado) throw new IllegalStateException("Processo não autuado");
                    validarIntegridade(c.documentos());
                    aplicarNovoEvento(new DocumentosJuntados(UUID.randomUUID(), id, List.copyOf(c.documentos()), Instant.now()));
                }
                case ProferirDecisao c -> {
                    if (!autuado) throw new IllegalStateException("Processo não autuado");
                    byte[] hash = gerarHash(c.teor());
                    aplicarNovoEvento(new DecisaoProferida(UUID.randomUUID(), id, hash, c.magistradoId(), Instant.now()));
                }
            }
        }

        public List<EventoJudicial> getMudancasNaoSalvas() {
            return List.copyOf(mudancasNaoSalvas);
        }

        public long versao() {
            return versao;
        }

        private void aplicarNovoEvento(EventoJudicial evento) {
            aplicarMutacao(evento);
            mudancasNaoSalvas.add(evento);
        }

        private void aplicarMutacao(EventoJudicial evento) {
            switch (evento) {
                case ProcessoAutuado ignored -> autuado = true;
                case DocumentosJuntados dj -> documentosAtivos.addAll(dj.documentos());
                case DecisaoProferida ignored -> {
                }
            }
        }

        private static void validarIntegridade(List<DocumentoMetadata> docs) {
            for (DocumentoMetadata d : docs) {
                if (d.hashSha384() == null || d.hashSha384().length() != 96) {
                    throw new SecurityException("hash_sha384 inválido: " + d.docId());
                }
            }
        }
    }

    private final ProcessEventStore eventStore;
    private final ObjectMapper mapper;
    private final PjbExecutionOrchestrator executionOrchestrator;

    public JudiciarioEventSourcingEngine(ProcessEventStore eventStore,
                                         ObjectMapper mapper,
                                         PjbExecutionOrchestrator executionOrchestrator) {
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
    }

    public CompletableFuture<Void> despachar(Comando comando) {
        Objects.requireNonNull(comando, "comando");
        return executionOrchestrator.run(PjbExecutionDescriptor.burst("judiciario-event-sourcing.dispatch", DISPATCH_TIMEOUT), () -> {
            long processoId = comando.processoId();

            List<EventoJudicial> historico = loadHistory(processoId);
            ProcessoAggregate aggregate = new ProcessoAggregate(processoId);
            if (!historico.isEmpty()) {
                aggregate.carregarHistorico(historico);
            }

            aggregate.processar(comando);

            List<EventoJudicial> mudancas = aggregate.getMudancasNaoSalvas();
            for (int i = 0; i < mudancas.size(); i++) {
                persist(processoId, mudancas.get(i));
            }
        });
    }

    private List<EventoJudicial> loadHistory(long processoId) {
        List<ProcessEventEnvelope> envs = eventStore.stream(processoId);
        List<EventoJudicial> out = new ArrayList<>(envs.size());
        for (int i = 0; i < envs.size(); i++) {
            try {
                EventoJudicial event = mapEnvelope(envs.get(i));
                if (event != null) {
                    out.add(event);
                }
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private EventoJudicial mapEnvelope(ProcessEventEnvelope e) throws Exception {
        String type = e.getEventType();
        if (ProcessEventType.PROCESS_AUTUADO.name().equals(type)) {
            return mapper.readValue(e.getPayload(), ProcessoAutuado.class);
        }
        if (ProcessEventType.DOCUMENTS_BULK_ADDED.name().equals(type)) {
            return mapper.readValue(e.getPayload(), DocumentosJuntados.class);
        }
        if (ProcessEventType.DECISION_ISSUED.name().equals(type)) {
            return mapper.readValue(e.getPayload(), DecisaoProferida.class);
        }
        return null;
    }

    private void persist(long processoId, EventoJudicial ev) {
        ProcessEventType type = switch (ev) {
            case ProcessoAutuado ignored -> ProcessEventType.PROCESS_AUTUADO;
            case DocumentosJuntados ignored -> ProcessEventType.DOCUMENTS_BULK_ADDED;
            case DecisaoProferida ignored -> ProcessEventType.DECISION_ISSUED;
        };
        eventStore.append(processoId, type, ev);
    }

    private static byte[] gerarHash(String conteudo) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-384");
            return d.digest(conteudo.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("sha-384 indisponível", e);
        }
    }
}
